package com.qq.ijay997.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.outbox.entity.RemoteMessage;
import com.qq.ijay997.outbox.entity.RemoteMessageLog;
import com.qq.ijay997.outbox.enums.BizType;
import com.qq.ijay997.outbox.enums.MessageStatus;
import com.qq.ijay997.outbox.repository.RemoteMessageLogRepository;
import com.qq.ijay997.outbox.repository.RemoteMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 本地消息表核心服务（Transactional Outbox）。
 * <p>
 * 职责：
 * <ul>
 *   <li>{@link #enqueue} 在业务同一事务内持久化待发送消息并发布入队事件；</li>
 *   <li>{@link #dispatch} 抢占待发送消息（CAS PENDING-&gt;SENDING）并批量调用远端；</li>
 *   <li>{@link #retry}/{@link #retryAllFailed} 支持失败消息手动重试；</li>
 *   <li>每次状态变更均写入 {@link RemoteMessageLog}，实现全链路追踪。</li>
 * </ul>
 *
 * @author ijay997
 */
@Slf4j
@Service
public class OutboxService {

    private final RemoteMessageRepository messageRepository;
    private final RemoteMessageLogRepository logRepository;
    private final RemoteApiGateway gateway;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final RemoteCallProperties properties;

    public OutboxService(RemoteMessageRepository messageRepository,
                         RemoteMessageLogRepository logRepository,
                         RemoteApiGateway gateway,
                         ApplicationEventPublisher eventPublisher,
                         ObjectMapper objectMapper,
                         TransactionTemplate transactionTemplate,
                         RemoteCallProperties properties) {
        this.messageRepository = messageRepository;
        this.logRepository = logRepository;
        this.gateway = gateway;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    /**
     * 消息入队：在调用方事务内执行（默认 REQUIRED 传播，加入主业务事务），
     * 保证「消息表写入」与「主业务写入」原子一致。
     */
    @Transactional
    public RemoteMessage enqueue(BizType bizType, String bizId, Object payload) {
        RemoteMessage message = new RemoteMessage();
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setPayload(toJson(payload));
        message.setStatus(MessageStatus.PENDING);
        message.setRetryCount(0);
        message.setCreateTime(LocalDateTime.now());
        messageRepository.save(message);

        appendLog(message.getId(), null, MessageStatus.PENDING, "消息入队");
        eventPublisher.publishEvent(new RemoteMessageEvent(message.getId()));
        log.info("本地消息已入队：id={}, bizType={}, bizId={}", message.getId(), bizType, bizId);
        return message;
    }

    /**
     * 调度发送：抢占一批待发送消息后统一调用远端接口。
     * <p>
     * 抢占（PENDING-&gt;SENDING）在独立短事务内完成，远端调用与结果落库在事务外执行，
     * 避免在网络调用期间长时间持有数据库事务。
     *
     * @return 本批次实际发送的消息条数
     */
    public int dispatch() {
        List<RemoteMessage> claimed = transactionTemplate.execute(status -> claimPending());
        if (claimed == null || claimed.isEmpty()) {
            return 0;
        }

        RemoteApiResult result;
        try {
            result = gateway.sendBatch(claimed);
        } catch (Exception e) {
            log.error("远端接口调用异常，共 {} 条消息", claimed.size(), e);
            result = RemoteApiResult.failure(e.getMessage());
        }

        for (RemoteMessage message : claimed) {
            if (result.isSuccess()) {
                markSuccess(message);
            } else {
                markFailed(message, result.getError());
            }
        }
        return claimed.size();
    }

    /**
     * 手动重试单条失败消息：FAILED -&gt; PENDING，由调用方随后触发 dispatch。
     */
    @Transactional
    public boolean retry(Long messageId) {
        RemoteMessage message = messageRepository.findById(messageId).orElse(null);
        if (message == null || message.getStatus() != MessageStatus.FAILED) {
            return false;
        }
        message.setStatus(MessageStatus.PENDING);
        message.setErrorMsg(null);
        message.setUpdateTime(LocalDateTime.now());
        messageRepository.save(message);
        appendLog(message.getId(), MessageStatus.FAILED, MessageStatus.PENDING, "手动重试：重置为待发送");
        return true;
    }

    /**
     * 批量重试所有失败消息。
     *
     * @return 重置为待发送的消息条数
     */
    @Transactional
    public int retryAllFailed() {
        List<RemoteMessage> failed = messageRepository.findByStatus(MessageStatus.FAILED);
        LocalDateTime now = LocalDateTime.now();
        for (RemoteMessage message : failed) {
            message.setStatus(MessageStatus.PENDING);
            message.setErrorMsg(null);
            message.setUpdateTime(now);
            messageRepository.save(message);
            appendLog(message.getId(), MessageStatus.FAILED, MessageStatus.PENDING, "批量重试：重置为待发送");
        }
        return failed.size();
    }

    /**
     * 消息列表查询，支持按状态与业务类型过滤（用于业务数据列表页面展示失败记录）。
     */
    public List<RemoteMessage> list(MessageStatus status, BizType bizType) {
        return messageRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> bizType == null || m.getBizType() == bizType)
                .collect(Collectors.toList());
    }

    /**
     * 查询单条消息的状态轨迹。
     */
    public List<RemoteMessageLog> logs(Long messageId) {
        return logRepository.findByMessageIdOrderByIdAsc(messageId);
    }

    private List<RemoteMessage> claimPending() {
        Pageable pageable = PageRequest.of(0, properties.getBatchSize());
        List<RemoteMessage> pending = messageRepository.findByStatusOrderByIdAsc(MessageStatus.PENDING, pageable);
        LocalDateTime now = LocalDateTime.now();
        List<RemoteMessage> claimed = new ArrayList<>(pending.size());
        for (RemoteMessage message : pending) {
            int updated = messageRepository.transitionStatus(
                    message.getId(), MessageStatus.PENDING, MessageStatus.SENDING, now);
            if (updated > 0) {
                message.setStatus(MessageStatus.SENDING);
                message.setUpdateTime(now);
                appendLog(message.getId(), MessageStatus.PENDING, MessageStatus.SENDING, "抢占成功，开始发送");
                claimed.add(message);
            }
        }
        return claimed;
    }

    private void markSuccess(RemoteMessage message) {
        message.setStatus(MessageStatus.SUCCESS);
        message.setErrorMsg(null);
        message.setUpdateTime(LocalDateTime.now());
        messageRepository.save(message);
        appendLog(message.getId(), MessageStatus.SENDING, MessageStatus.SUCCESS, "发送成功");
    }

    private void markFailed(RemoteMessage message, String error) {
        message.setStatus(MessageStatus.FAILED);
        message.setRetryCount(message.getRetryCount() == null ? 1 : message.getRetryCount() + 1);
        message.setErrorMsg(error);
        message.setUpdateTime(LocalDateTime.now());
        messageRepository.save(message);
        appendLog(message.getId(), MessageStatus.SENDING, MessageStatus.FAILED, "发送失败：" + error);
    }

    private void appendLog(Long messageId, MessageStatus from, MessageStatus to, String remark) {
        RemoteMessageLog logEntry = new RemoteMessageLog();
        logEntry.setMessageId(messageId);
        logEntry.setFromStatus(from);
        logEntry.setToStatus(to);
        logEntry.setRemark(remark);
        logEntry.setCreateTime(LocalDateTime.now());
        logRepository.save(logEntry);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化远端请求参数失败", e);
        }
    }
}