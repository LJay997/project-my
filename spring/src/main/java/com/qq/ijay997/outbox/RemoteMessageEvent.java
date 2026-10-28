package com.qq.ijay997.outbox;

import org.springframework.context.ApplicationEvent;

/**
 * 本地消息入队事件。由 {@link OutboxService} 在业务事务内发布，
 * 通过 {@code @TransactionalEventListener(AFTER_COMMIT)} 在事务提交后异步触发发送。
 *
 * @author ijay997
 */
public class RemoteMessageEvent extends ApplicationEvent {

    private final Long messageId;

    public RemoteMessageEvent(Long messageId) {
        super(messageId);
        this.messageId = messageId;
    }

    public Long getMessageId() {
        return messageId;
    }
}