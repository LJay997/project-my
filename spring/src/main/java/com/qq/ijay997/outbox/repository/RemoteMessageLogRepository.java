package com.qq.ijay997.outbox.repository;

import com.qq.ijay997.outbox.entity.RemoteMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息状态变更日志数据访问层。
 *
 * @author ijay997
 */
@Repository
public interface RemoteMessageLogRepository extends JpaRepository<RemoteMessageLog, Long> {

    /**
     * 按消息主键查询其状态轨迹。
     */
    List<RemoteMessageLog> findByMessageIdOrderByIdAsc(Long messageId);
}