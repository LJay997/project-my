package com.qq.ijay997.outbox.entity;

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
 * 消息状态变更日志：记录每条消息从 入队 -&gt; 发送中 -&gt; 成功/失败 的完整轨迹。
 *
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_remote_message_log")
public class RemoteMessageLog {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的消息主键 */
    @Column(nullable = false)
    private Long messageId;

    /** 变更前状态（首次入队时为 null） */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MessageStatus fromStatus;

    /** 变更后状态 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus toStatus;

    /** 备注 */
    @Column(length = 500)
    private String remark;

    /** 创建时间 */
    @Column(nullable = false)
    private LocalDateTime createTime;
}