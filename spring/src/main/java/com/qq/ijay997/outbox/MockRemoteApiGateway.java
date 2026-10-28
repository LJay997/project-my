package com.qq.ijay997.outbox;

import com.qq.ijay997.outbox.entity.RemoteMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 远端接口的 Mock 实现，用于演示与测试。
 * <p>
 * 默认一律成功；可通过 {@link #setFailAll(boolean)} 模拟远端故障，
 * 便于验证失败标记与手动重试逻辑。生产环境替换为 Feign/HTTP 实现即可。
 *
 * @author ijay997
 */
@Slf4j
@Component
public class MockRemoteApiGateway implements RemoteApiGateway {

    private volatile boolean failAll = false;

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public RemoteApiResult sendBatch(List<RemoteMessage> messages) {
        callCount.incrementAndGet();
        if (failAll) {
            log.warn("Mock 远端接口不可用，本次 {} 条消息发送失败", messages.size());
            return RemoteApiResult.failure("远端服务暂不可用");
        }
        log.info("Mock 远端接口成功接收 {} 条消息", messages.size());
        return RemoteApiResult.success();
    }

    public void setFailAll(boolean failAll) {
        this.failAll = failAll;
    }

    public boolean isFailAll() {
        return failAll;
    }

    public int getCallCount() {
        return callCount.get();
    }

    /** 重置为可用的成功状态（供测试复用） */
    public void reset() {
        this.failAll = false;
        this.callCount.set(0);
    }
}