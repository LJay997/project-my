package com.qq.ijay997.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 事务监听器：监听本地消息入队事件。
 * <p>
 * 基于 Spring {@code @TransactionalEventListener}（即项目笔记中的事务监听机制），
 * 在业务事务 {@code AFTER_COMMIT} 之后异步触发批量发送，确保：
 * 1）主业务事务提交后才发送；2）发送在异步线程池中执行，不阻塞主流程。
 *
 * @author ijay997
 */
@Slf4j
@Component
public class RemoteCallTransactionListener {

    private final OutboxService outboxService;
    private final RemoteCallProperties properties;

    public RemoteCallTransactionListener(OutboxService outboxService, RemoteCallProperties properties) {
        this.outboxService = outboxService;
        this.properties = properties;
    }

    @Async("remoteCallExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageEnqueued(RemoteMessageEvent event) {
        if (!properties.isAutoDispatch()) {
            log.debug("关闭自动调度，跳过消息 id={}", event.getMessageId());
            return;
        }
        log.debug("事务提交后触发批量发送，消息 id={}", event.getMessageId());
        outboxService.dispatch();
    }
}