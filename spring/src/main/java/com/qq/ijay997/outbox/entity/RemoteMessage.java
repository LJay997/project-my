package com.qq.ijay997.outbox.entity;

import com.qq.ijay997.outbox.enums.BizType;
import com.qq.ijay997.outbox.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 本地消息表：持久化待发送的远端请求。
 * <p>
 * 与主业务数据在同一事务内写入（Transactional Outbox 模式），
 * 远端发送走异步调度，避免阻塞主业务流程。
 *
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_remote_message")
public class RemoteMessage {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务场景类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BizType bizType;

    /** 关联业务主键（如用户 ID，或导入批次标识） */
    @Column(nullable = false, length = 64)
    private String bizId;

    /** 序列化后的远端请求参数 */
    @Column(nullable = false, length = 4000)
    private String payload;

    /** 消息状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    /** 已重试次数 */
    @Column(nullable = false)
    private Integer retryCount;

    /** 最近一次失败原因 */
    @Column(length = 500)
    private String errorMsg;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column
    private LocalDateTime updateTime;
}