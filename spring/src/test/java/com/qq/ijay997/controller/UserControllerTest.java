package com.qq.ijay997.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * UserController 测试类
 * 
 * @author ijay997
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testUser = new User(1L, "张三", "zhangsan@example.com", 25);
    }

    /**
     * 测试创建用户 - 正常情况
     */
    @Test
    @DisplayName("测试创建用户 - 成功")
    void testCreateUser_Success() throws Exception {
        // 模拟 Service 层行为
        User savedUser = new User(1L, "张三", "zhangsan@example.com", 25);
        given(userService.save(any(User.class))).willReturn(savedUser);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isCreated())           // 期望返回 201 状态码
                .andExpect(jsonPath("$.id").value(1))      // 验证返回的 ID
                .andExpect(jsonPath("$.username").value("张三"))  // 验证用户名
                .andExpect(jsonPath("$.email").value("zhangsan@example.com"))  // 验证邮箱
                .andExpect(jsonPath("$.age").value(25));   // 验证年龄

        // 验证 Service 方法被调用了一次
        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 用户名为空
     */
    @Test
    @DisplayName("测试创建用户 - 用户名为空")
    void testCreateUser_EmptyUsername() throws Exception {
        // 准备测试数据（用户名为空）
        User invalidUser = new User(null, "", "test@example.com", 25);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());  // 期望返回 400 状态码

        // 验证 Service 方法仍然被调用（实际项目中应该在 Controller 层做参数校验）
        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 邮箱为空
     */
    @Test
    @DisplayName("测试创建用户 - 邮箱为空")
    void testCreateUser_EmptyEmail() throws Exception {
        // 准备测试数据（邮箱为空）
        User invalidUser = new User(null, "李四", "", 30);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 年龄为负数
     */
    @Test
    @DisplayName("测试创建用户 - 年龄无效")
    void testCreateUser_InvalidAge() throws Exception {
        // 准备测试数据（年龄为负数）
        User invalidUser = new User(null, "王五", "wangwu@example.com", -5);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - Service 抛出异常
     */
    @Test
    @DisplayName("测试创建用户 - Service 异常")
    void testCreateUser_ServiceException() throws Exception {
        // 模拟 Service 层抛出异常
        given(userService.save(any(User.class)))
                .willThrow(new RuntimeException("数据库错误"));

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isBadRequest());  // 期望返回 400 状态码

        // 验证 Service 方法被调用了一次
        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 邮箱格式不正确
     */
    @Test
    @DisplayName("测试创建用户 - 邮箱格式错误")
    void testCreateUser_InvalidEmailFormat() throws Exception {
        // 准备测试数据（邮箱格式错误）
        User invalidUser = new User(null, "赵六", "invalid-email", 28);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 重复的用户名
     */
    @Test
    @DisplayName("测试创建用户 - 用户名重复")
    void testCreateUser_DuplicateUsername() throws Exception {
        // 模拟 Service 层检测到重复用户名
        given(userService.save(any(User.class)))
                .willThrow(new IllegalArgumentException("用户名已存在"));

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 完整的用户对象
     */
    @Test
    @DisplayName("测试创建用户 - 完整数据")
    void testCreateUser_CompleteData() throws Exception {
        // 准备完整的测试数据
        User completeUser = new User(null, "孙七", "sunqi@example.com", 35);
        User savedUser = new User(2L, "孙七", "sunqi@example.com", 35);
        
        given(userService.save(any(User.class))).willReturn(savedUser);

        // 执行请求并验证所有字段
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("孙七"))
                .andExpect(jsonPath("$.email").value("sunqi@example.com"))
                .andExpect(jsonPath("$.age").value(35))
                .andDo(print());  // 打印详细的请求响应信息

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 边界值测试（最大年龄）
     */
    @Test
    @DisplayName("测试创建用户 - 边界值：最大年龄")
    void testCreateUser_MaxAge() throws Exception {
        // 准备测试数据（最大合理年龄）
        User oldUser = new User(null, "老人", "old@example.com", 150);
        User savedUser = new User(3L, "老人", "old@example.com", 150);
        
        given(userService.save(any(User.class))).willReturn(savedUser);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.age").value(150));

        verify(userService, times(1)).save(any(User.class));
    }

    /**
     * 测试创建用户 - 边界值测试（最小年龄 0 岁）
     */
    @Test
    @DisplayName("测试创建用户 - 边界值：最小年龄")
    void testCreateUser_MinAge() throws Exception {
        // 准备测试数据（最小年龄）
        User babyUser = new User(null, "婴儿", "baby@example.com", 0);
        User savedUser = new User(4L, "婴儿", "baby@example.com", 0);
        
        given(userService.save(any(User.class))).willReturn(savedUser);

        // 执行请求并验证结果
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(babyUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.age").value(0));

        verify(userService, times(1)).save(any(User.class));
    }
}
