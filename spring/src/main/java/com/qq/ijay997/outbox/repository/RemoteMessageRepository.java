package com.qq.ijay997.outbox.repository;

import com.qq.ijay997.outbox.entity.RemoteMessage;
import com.qq.ijay997.outbox.enums.BizType;
import com.qq.ijay997.outbox.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表数据访问层。
 *
 * @author ijay997
 */
@Repository
public interface RemoteMessageRepository extends JpaRepository<RemoteMessage, Long> {

    /**
     * 按状态 + ID 升序分页查询（用于按批次抢占待发送消息，FIFO）。
     */
    List<RemoteMessage> findByStatusOrderByIdAsc(MessageStatus status, Pageable pageable);

    /**
     * 按状态查询全部。
     */
    List<RemoteMessage> findByStatus(MessageStatus status);

    /**
     * 按业务类型查询。
     */
    List<RemoteMessage> findByBizType(BizType bizType);

    /**
     * 原子状态迁移（CAS）：仅当消息仍处于 from 状态时迁移到 to 状态。
     * <p>
     * 通过条件更新避免并发调度时同一消息被重复抢占。
     *
     * @return 受影响行数（0 表示已被其他调度抢占）
     */
    @Modifying
    @Query("update RemoteMessage m set m.status = :to, m.updateTime = :now "
            + "where m.id = :id and m.status = :from")
    int transitionStatus(@Param("id") Long id,
                         @Param("from") MessageStatus from,
                         @Param("to") MessageStatus to,
                         @Param("now") LocalDateTime now);
}