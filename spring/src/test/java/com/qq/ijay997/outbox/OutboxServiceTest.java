package com.qq.ijay997.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.outbox.entity.RemoteMessage;
import com.qq.ijay997.outbox.entity.RemoteMessageLog;
import com.qq.ijay997.outbox.enums.BizType;
import com.qq.ijay997.outbox.enums.MessageStatus;
import com.qq.ijay997.outbox.repository.RemoteMessageLogRepository;
import com.qq.ijay997.outbox.repository.RemoteMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 本地消息表核心服务的单元测试：覆盖入队、正常发送、失败标记、手动重试等核心逻辑。
 *
 * @author ijay997
 */
@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private RemoteMessageRepository messageRepository;

    @Mock
    private RemoteMessageLogRepository logRepository;

    @Mock
    private RemoteApiGateway gateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RemoteCallProperties properties = new RemoteCallProperties();

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(messageRepository, logRepository, gateway,
                eventPublisher, objectMapper, transactionTemplate, properties);
        // 单元测试中使用假的事务模板：直接同步执行回调，避免依赖真实事务
        // 使用 lenient 允许部分用例不触发该 stub
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("入队：以 PENDING 状态持久化并发布事件")
    void shouldEnqueueAsPendingAndPublishEvent() {
        when(messageRepository.save(any(RemoteMessage.class))).thenAnswer(invocation -> {
            RemoteMessage m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });

        RemoteMessage message = outboxService.enqueue(
                BizType.CREATE, "1", new HashMap<String, Object>() {{ put("name", "张三"); }});

        assertAll(
                () -> assertNotNull(message.getId()),
                () -> assertEquals(MessageStatus.PENDING, message.getStatus()),
                () -> assertEquals(BizType.CREATE, message.getBizType()),
                () -> assertEquals(0, message.getRetryCount())
        );
        verify(messageRepository).save(any(RemoteMessage.class));
        verify(logRepository).save(any(RemoteMessageLog.class));
        verify(eventPublisher).publishEvent(any(RemoteMessageEvent.class));
    }

    @Test
    @DisplayName("调度：远端成功时消息流转为 SUCCESS")
    void shouldMarkSuccessOnRemoteSuccess() {
        RemoteMessage pending = message(MessageStatus.PENDING);
        when(messageRepository.findByStatusOrderByIdAsc(eq(MessageStatus.PENDING), any()))
                .thenReturn(Collections.singletonList(pending));
        when(messageRepository.transitionStatus(eq(1L), eq(MessageStatus.PENDING),
                eq(MessageStatus.SENDING), any(LocalDateTime.class))).thenReturn(1);
        when(gateway.sendBatch(any())).thenReturn(RemoteApiResult.success());

        int dispatched = outboxService.dispatch();

        assertEquals(1, dispatched);
        ArgumentCaptor<RemoteMessage> captor = ArgumentCaptor.forClass(RemoteMessage.class);
        verify(messageRepository).save(captor.capture());
        assertEquals(MessageStatus.SUCCESS, captor.getValue().getStatus());
        verify(logRepository, atLeastOnce()).save(any(RemoteMessageLog.class));
    }

    @Test
    @DisplayName("调度：远端失败时消息标记为 FAILED 并自增重试次数")
    void shouldMarkFailedAndIncrementRetryCount() {
        RemoteMessage pending = message(MessageStatus.PENDING);
        when(messageRepository.findByStatusOrderByIdAsc(eq(MessageStatus.PENDING), any()))
                .thenReturn(Collections.singletonList(pending));
        when(messageRepository.transitionStatus(eq(1L), eq(MessageStatus.PENDING),
                eq(MessageStatus.SENDING), any(LocalDateTime.class))).thenReturn(1);
        when(gateway.sendBatch(any())).thenReturn(RemoteApiResult.failure("下游服务超时"));

        outboxService.dispatch();

        ArgumentCaptor<RemoteMessage> captor = ArgumentCaptor.forClass(RemoteMessage.class);
        verify(messageRepository).save(captor.capture());
        RemoteMessage saved = captor.getValue();
        assertAll(
                () -> assertEquals(MessageStatus.FAILED, saved.getStatus()),
                () -> assertEquals(1, saved.getRetryCount()),
                () -> assertEquals("下游服务超时", saved.getErrorMsg())
        );
    }

    @Test
    @DisplayName("调度：远端抛异常时同样标记为 FAILED")
    void shouldMarkFailedWhenGatewayThrows() {
        RemoteMessage pending = message(MessageStatus.PENDING);
        when(messageRepository.findByStatusOrderByIdAsc(eq(MessageStatus.PENDING), any()))
                .thenReturn(Collections.singletonList(pending));
        when(messageRepository.transitionStatus(eq(1L), eq(MessageStatus.PENDING),
                eq(MessageStatus.SENDING), any(LocalDateTime.class))).thenReturn(1);
        when(gateway.sendBatch(any())).thenThrow(new RuntimeException("网络异常"));

        outboxService.dispatch();

        ArgumentCaptor<RemoteMessage> captor = ArgumentCaptor.forClass(RemoteMessage.class);
        verify(messageRepository).save(captor.capture());
        assertAll(
                () -> assertEquals(MessageStatus.FAILED, captor.getValue().getStatus()),
                () -> assertTrue(captor.getValue().getErrorMsg().contains("网络异常"))
        );
    }

    @Test
    @DisplayName("调度：无待发送消息时不触发远端调用")
    void shouldNotSendWhenNoPending() {
        when(messageRepository.findByStatusOrderByIdAsc(eq(MessageStatus.PENDING), any()))
                .thenReturn(Collections.emptyList());

        int dispatched = outboxService.dispatch();

        assertEquals(0, dispatched);
        verify(gateway, never()).sendBatch(any());
    }

    @Test
    @DisplayName("手动重试：FAILED 重置为 PENDING；非法状态返回 false")
    void shouldRetryFailedMessage() {
        RemoteMessage failed = message(MessageStatus.FAILED);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(failed));

        boolean retried = outboxService.retry(1L);

        assertTrue(retried);
        assertEquals(MessageStatus.PENDING, failed.getStatus());

        // 非 FAILED 状态不允许重试
        assertFalse(outboxService.retry(2L));
    }

    private RemoteMessage message(MessageStatus status) {
        RemoteMessage m = new RemoteMessage();
        m.setId(1L);
        m.setBizType(BizType.CREATE);
        m.setBizId("1");
        m.setPayload("{}");
        m.setStatus(status);
        m.setRetryCount(0);
        m.setCreateTime(LocalDateTime.now());
        return m;
    }
}