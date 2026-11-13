好的，下面是按 CAS 方案最终确定并固定下来的实现方案，可以直接照此实现。

## 一、表结构（达梦兼容写法）

```sql
CREATE TABLE T_OUTBOX_MESSAGE (
  ID              BIGINT       IDENTITY(1,1) PRIMARY KEY,
  EVENT_TYPE      VARCHAR(64)  NOT NULL,
  BIZ_ID          VARCHAR(64)  NOT NULL,
  PAYLOAD         CLOB         NOT NULL,
  STATUS          TINYINT      DEFAULT 0 NOT NULL,   -- 0待发送 1成功 2待重试 3发送中 4死信
  RETRY_COUNT     INT          DEFAULT 0 NOT NULL,
  MAX_RETRY       INT          DEFAULT 5 NOT NULL,
  SEND_TOKEN      VARCHAR(64),
  NEXT_RETRY_TIME TIMESTAMP,
  LAST_ERROR      VARCHAR(500),
  CREATE_TIME     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
  UPDATE_TIME     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT UK_OUTBOX_EVENT UNIQUE (EVENT_TYPE, BIZ_ID)
);
CREATE INDEX IDX_OUTBOX_SCAN ON T_OUTBOX_MESSAGE (STATUS, NEXT_RETRY_TIME);
```

## 二、状态机（最终版）

```
INIT(0) ──claim──► SENDING(3) ──成功──► SUCCESS(1)
                     │
                     ├─失败且 retry<max ──► FAIL(2) ──claim──► SENDING(3)
                     └─失败且 retry≥max ──► DEAD(4)

SENDING(3) ──看门狗超时(update_time 超 10min)──► FAIL(2)
DEAD(4) ──人工重推──► FAIL(2)
```

## 三、业务侧：只发布事件

```java
@Transactional
public void approve(Long projectId) {
    projectMapper.updateStatus(projectId, "APPROVED");
    publisher.publishEvent(new ProjectChangedEvent(
            "PROJECT_APPROVED",
            String.valueOf(projectId),
            JsonUtils.toJson(projectMapper.selectById(projectId))));
}
```

## 四、监听器：同事务落库 + 注册 afterCommit

```java
@Component
@RequiredArgsConstructor
public class OutboxListener {

    private final OutboxMapper outboxMapper;
    private final OutboxPushService pushService;
    private final TaskExecutor ipdExecutor;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onProjectChanged(ProjectChangedEvent event) {
        OutboxMessage msg = OutboxMessage.of(event);
        outboxMapper.insert(msg);                 // 与业务同事务，id 回填

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        ipdExecutor.execute(() -> pushService.push(msg.getId()));
                    } catch (RejectedExecutionException e) {
                        log.warn("ipd executor rejected, msgId={}", msg.getId());
                    }
                }
            });
    }
}
```

约束：监听器**同步**、不加 `@Async`；业务方法必须有 `@Transactional`，否则事件不会触发。

## 五、推送服务：CAS 抢占 + token 回写

```java
@Service
@RequiredArgsConstructor
public class OutboxPushService {

    private final OutboxMapper mapper;
    private final IpdClient ipdClient;

    private static final int MAX_RETRY = 5;
    private static final long MAX_BACKOFF_MINUTES = 60;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void push(Long id) {
        String token = UUID.randomUUID().toString();
        if (mapper.claim(id, token, MAX_RETRY) != 1) {
            return;                                // 抢占失败，跳过
        }
        OutboxMessage msg = mapper.selectById(id);
        try {
            ipdClient.send(msg.getEventType(), msg.getPayload());
            mapper.markSuccess(id, token);
        } catch (Exception e) {
            int nextRetry = msg.getRetryCount() + 1;
            long backoff = Math.min(1L << nextRetry, MAX_BACKOFF_MINUTES);
            boolean dead = nextRetry >= MAX_RETRY;
            mapper.markFail(id, token, nextRetry,
                    LocalDateTime.now().plusMinutes(backoff),
                    dead, truncate(e.getMessage(), 500));
        }
    }
}
```

Mapper 关键 SQL：

```xml
<update id="claim">
  UPDATE T_OUTBOX_MESSAGE
  SET STATUS = 3, SEND_TOKEN = #{token}, UPDATE_TIME = CURRENT_TIMESTAMP
  WHERE ID = #{id} AND STATUS IN (0, 2) AND RETRY_COUNT &lt; #{maxRetry}
</update>

<update id="markSuccess">
  UPDATE T_OUTBOX_MESSAGE
  SET STATUS = 1, LAST_ERROR = NULL, SEND_TOKEN = NULL, UPDATE_TIME = CURRENT_TIMESTAMP
  WHERE ID = #{id} AND STATUS = 3 AND SEND_TOKEN = #{token}
</update>

<update id="markFail">
  UPDATE T_OUTBOX_MESSAGE
  SET STATUS = #{dead,jdbcType=TINYINT} ,
      RETRY_COUNT = #{retryCount}, NEXT_RETRY_TIME = #{nextRetryTime},
      LAST_ERROR = #{error}, SEND_TOKEN = NULL, UPDATE_TIME = CURRENT_TIMESTAMP
  WHERE ID = #{id} AND STATUS = 3 AND SEND_TOKEN = #{token}
</update>
```

`dead` 传 1/0，SQL 里用 `CASE WHEN #{dead} = 1 THEN 4 ELSE 2 END`（达梦不支持 `IF()`）。

## 六、定时任务 + 看门狗

```java
@Scheduled(fixedDelay = 60_000)
public void compensate() {
    mapper.selectPending(100).forEach(pushService::push);
}
```

```xml
<select id="selectPending" resultType="long">
  SELECT ID FROM T_OUTBOX_MESSAGE
  WHERE STATUS IN (0, 2)
    AND RETRY_COUNT &lt; MAX_RETRY
    AND NEXT_RETRY_TIME &lt;= CURRENT_TIMESTAMP
  ORDER BY NEXT_RETRY_TIME
  LIMIT 100
</select>
```

看门狗（每 5 分钟）：

```sql
UPDATE T_OUTBOX_MESSAGE
SET STATUS = 2, LAST_ERROR = 'send timeout', SEND_TOKEN = NULL
WHERE STATUS = 3 AND UPDATE_TIME < CURRENT_TIMESTAMP - 600 SECOND;
```

## 七、参数建议

| 参数 | 建议值 | 依据 |
|---|---|---|
| HTTP 连接/读取超时 | 3s / 10s | 必须小于看门狗阈值 |
| 看门狗阈值 | 10 分钟 | 大于最大发送耗时 |
| 最大重试次数 | 5 | 重要事件 5，次要事件 2–3 |
| 退避 | 2^n 分钟，上限 60 | 避免打爆下游 |
| 扫描批量 | 100 条/批 | 控制单轮耗时 |
| 线程池 | 核心 4、队列 200、AbortPolicy | 拒绝后由补偿兜底 |

## 八、上线自检清单

- [ ] 业务方法与消息 insert 在同一事务，回滚时消息一起消失
- [ ] 事件重复发布不会导致业务事务回滚（达梦用 `MERGE INTO` 或先查后插兜底唯一键）
- [ ] 监听器没有 `@Async`
- [ ] `afterCommit` 里不做 DB 写，状态回写走 `REQUIRES_NEW`
- [ ] `markSuccess` / `markFail` 都带 `SEND_TOKEN` 条件
- [ ] 看门狗阈值 > HTTP 超时
- [ ] 死信有告警（企业微信/邮件），不只落库
- [ ] 成功消息定期归档（按 create_time 归档 30 天前）

## 九、简历最终表述

> 基于本地消息表实现 IPD 推送最终一致性：业务事务内通过 Spring Event 同步监听器写入消息表，注册 afterCommit 回调异步推送（携带消息快照避免回查）；消息采用「待发送/发送中/成功/待重试/死信」五态模型，用条件更新 + `send_token` 做 CAS 抢占，唯一键与退避重试保证幂等，看门狗兜底发送超时，失败消息定时补偿。

这套方案的关键就一句话：**状态即锁，条件更新即抢占，token 防迟到覆盖。**