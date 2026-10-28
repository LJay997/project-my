package com.qq.ijay997.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 事务监听器单元测试：验证自动调度开关对 {@code dispatch} 的触发逻辑。
 *
 * @author ijay997
 */
@ExtendWith(MockitoExtension.class)
class RemoteCallTransactionListenerTest {

    @Mock
    private OutboxService outboxService;

    private final RemoteCallProperties properties = new RemoteCallProperties();

    private RemoteCallTransactionListener listener;

    @BeforeEach
    void setUp() {
        listener = new RemoteCallTransactionListener(outboxService, properties);
    }

    @Test
    @DisplayName("自动调度开启：事务提交后触发批量发送")
    void shouldDispatchWhenAutoDispatchEnabled() {
        properties.setAutoDispatch(true);

        listener.onMessageEnqueued(new RemoteMessageEvent(1L));

        verify(outboxService).dispatch();
    }

    @Test
    @DisplayName("自动调度关闭：不触发批量发送")
    void shouldSkipWhenAutoDispatchDisabled() {
        properties.setAutoDispatch(false);

        listener.onMessageEnqueued(new RemoteMessageEvent(1L));

        verify(outboxService, never()).dispatch();
    }
}