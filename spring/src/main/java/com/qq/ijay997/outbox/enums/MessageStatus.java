package com.qq.ijay997.outbox.enums;

/**
 * 远端调用消息的全链路状态。
 * <p>
 * 状态流转：PENDING -&gt; SENDING -&gt; SUCCESS / FAILED；FAILED 可经手动重试回到 PENDING。
 *
 * @author ijay997
 */
public enum MessageStatus {

    /** 待发送 */
    PENDING("待发送"),

    /** 发送中（已被调度线程抢占） */
    SENDING("发送中"),

    /** 发送成功 */
    SUCCESS("发送成功"),

    /** 发送失败 */
    FAILED("发送失败");

    private final String desc;

    MessageStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}