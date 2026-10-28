package com.qq.ijay997.outbox;

import com.qq.ijay997.entity.User;
import com.qq.ijay997.outbox.entity.RemoteMessage;
import com.qq.ijay997.outbox.enums.BizType;
import com.qq.ijay997.outbox.enums.MessageStatus;
import com.qq.ijay997.outbox.repository.RemoteMessageLogRepository;
import com.qq.ijay997.outbox.repository.RemoteMessageRepository;
import com.qq.ijay997.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 远端调用本地消息表的集成测试。
 * <p>
 * 关闭自动异步调度（{@code remote-call.auto-dispatch=false}），
 * 通过手动调用 {@link OutboxService#dispatch()} 确保断言确定性，
 * 核心覆盖：四类业务场景入队、正常发送、失败自动标记、手动重试、事务一致性。
 *
 * @author ijay997
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "remote-call.auto-dispatch=false")
class RemoteCallIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private RemoteMessageRepository messageRepository;

    @Autowired
    private RemoteMessageLogRepository logRepository;

    @Autowired
    private MockRemoteApiGateway mockGateway;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        mockGateway.reset();
        logRepository.deleteAll();
        messageRepository.deleteAll();
    }

    @Test
    @DisplayName("四类业务场景：新建/编辑/删除/导入均在同一事务内写入消息")
    void shouldEnqueueMessageForEachBusinessScenario() {
        // 数据新建
        User created = userService.save(new User(null, "rc-新建", "rc.create@test.com", 20));
        // 数据编辑
        userService.update(created.getId(), new User(created.getId(), "rc-编辑", "rc.update@test.com", 21));
        // 数据删除
        userService.deleteById(created.getId());
        // 文件导入
        userService.importUser(new User(null, "rc-导入", "rc.import@test.com", 22));

        List<RemoteMessage> all = messageRepository.findAll();
        assertEquals(4, all.size(), "四种业务各自应产生一条消息");

        assertTrue(all.stream().anyMatch(m -> m.getBizType() == BizType.CREATE));
        assertTrue(all.stream().anyMatch(m -> m.getBizType() == BizType.UPDATE));
        assertTrue(all.stream().anyMatch(m -> m.getBizType() == BizType.DELETE));
        assertTrue(all.stream().anyMatch(m -> m.getBizType() == BizType.IMPORT));
        // 未调度时全部处于待发送，且每条消息都有入队日志
        assertTrue(all.stream().allMatch(m -> m.getStatus() == MessageStatus.PENDING));
        all.forEach(m -> assertTrue(logRepository.findByMessageIdOrderByIdAsc(m.getId()).size() >= 1,
                "每条消息至少有一条入队状态日志"));
    }

    @Test
    @DisplayName("正常发送：调度后消息流水为 待发送→发送中→发送成功")
    void shouldDispatchSuccessfully() {
        User saved = userService.save(new User(null, "rc-发送", "rc.send@test.com", 30));

        int dispatched = outboxService.dispatch();

        assertEquals(1, dispatched);
        RemoteMessage message = messageRepository.findAll().get(0);
        assertEquals(MessageStatus.SUCCESS, message.getStatus());
        assertTrue(logRepository.findByMessageIdOrderByIdAsc(message.getId()).stream()
                        .anyMatch(log -> log.getToStatus() == MessageStatus.SUCCESS),
                "状态日志应包含发送成功");
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("发送失败自动标记：消息置为 FAILED 并记录失败原因与重试次数")
    void shouldMarkFailedWhenRemoteFails() {
        userService.save(new User(null, "rc-失败", "rc.fail@test.com", 30));
        mockGateway.setFailAll(true);

        outboxService.dispatch();

        RemoteMessage message = messageRepository.findAll().get(0);
        assertEquals(MessageStatus.FAILED, message.getStatus());
        assertEquals(1, message.getRetryCount());
        assertNotNull(message.getErrorMsg());
        assertTrue(logRepository.findByMessageIdOrderByIdAsc(message.getId()).stream()
                        .anyMatch(log -> log.getToStatus() == MessageStatus.FAILED),
                "状态日志应包含发送失败");
    }

    @Test
    @DisplayName("手动重试：失败消息经接口重试后恢复为发送成功")
    void shouldRetryFailedMessageViaEndpoint() throws Exception {
        userService.save(new User(null, "rc-重试", "rc.retry@test.com", 30));
        mockGateway.setFailAll(true);
        outboxService.dispatch();

        Long messageId = messageRepository.findAll().get(0).getId();
        assertEquals(MessageStatus.FAILED, messageRepository.findById(messageId).get().getStatus());

        // 恢复远端可用后手动重试
        mockGateway.setFailAll(false);
        mockMvc.perform(post("/api/remote-messages/" + messageId + "/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        assertEquals(MessageStatus.SUCCESS, messageRepository.findById(messageId).get().getStatus());
    }

    @Test
    @DisplayName("事务一致性：业务事务回滚时消息一并回滚，不产生脏消息")
    void shouldRollbackMessageWhenBusinessTransactionFails() {
        long before = messageRepository.count();

        assertThrows(IllegalStateException.class, () -> transactionTemplate.execute(status -> {
            outboxService.enqueue(BizType.CREATE, "99999", new User(null, "rc-回滚", "rc.rollback@test.com", 20));
            throw new IllegalStateException("模拟业务失败");
        }));

        assertEquals(before, messageRepository.count(), "业务回滚时消息表应一并回滚");
    }
}