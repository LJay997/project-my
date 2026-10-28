package com.qq.ijay997.outbox;

import com.qq.ijay997.outbox.entity.RemoteMessage;

import java.util.List;

/**
 * 远端接口网关抽象。
 * <p>
 * 将「按批次组装请求参数后统一发起调用」封装在此接口内：
 * 一次 sendBatch 对应一批消息、一次远端调用。生产环境可用 Feign/HTTP 实现替换。
 *
 * @author ijay997
 */
public interface RemoteApiGateway {

    /**
     * 批量调用远端接口。
     *
     * @param messages 本批次待发送的消息（已组装好参数）
     * @return 调用结果
     */
    RemoteApiResult sendBatch(List<RemoteMessage> messages);
}