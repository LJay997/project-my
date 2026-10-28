package com.qq.ijay997.outbox;

import com.qq.ijay997.dto.Result;
import com.qq.ijay997.outbox.entity.RemoteMessage;
import com.qq.ijay997.outbox.enums.BizType;
import com.qq.ijay997.outbox.enums.MessageStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 远端调用消息管理接口。
 * <p>
 * 对应需求中的「业务数据列表页面」后端能力：查询消息（含失败记录）与手动重试。
 *
 * @author ijay997
 */
@RestController
@RequestMapping("/api/remote-messages")
public class RemoteMessageController {

    private final OutboxService outboxService;

    public RemoteMessageController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    /**
     * 查询消息列表，可按状态与业务类型过滤。
     */
    @GetMapping
    public Result<List<RemoteMessage>> list(
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) BizType bizType) {
        return Result.success(outboxService.list(status, bizType));
    }

    /**
     * 手动重试单条失败消息。
     */
    @PostMapping("/{id}/retry")
    public Result<Boolean> retry(@PathVariable Long id) {
        boolean retried = outboxService.retry(id);
        if (!retried) {
            return Result.error(404, "消息不存在或当前状态不允许重试");
        }
        // 重试为同步触发（用户主动操作），立即调度发送
        outboxService.dispatch();
        return Result.success(true);
    }

    /**
     * 批量重试所有失败消息。
     */
    @PostMapping("/retry-failed")
    public Result<Integer> retryFailed() {
        int count = outboxService.retryAllFailed();
        outboxService.dispatch();
        return Result.success(count);
    }
}