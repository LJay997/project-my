package com.qq.ijay997.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 集成测试
 * 
 * @author ijay997
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 每个测试前清空数据
    }

    /**
     * 测试创建用户的完整流程
     */
    @Test
    @DisplayName("集成测试：创建用户完整流程")
    void testCreateUser_Integration() throws Exception {
        // 准备测试数据
        User newUser = new User(null, "集成测试用户", "integration@example.com", 28);

        // 执行创建请求
        String responseContent = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("集成测试用户"))
                .andExpect(jsonPath("$.email").value("integration@example.com"))
                .andExpect(jsonPath("$.age").value(28))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 从响应中获取用户 ID
        Long userId = objectMapper.readTree(responseContent).get("id").asLong();

        // 验证创建的用户可以通过 ID 查询到
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("集成测试用户"));
    }

    /**
     * 测试获取所有用户
     */
    @Test
    @DisplayName("集成测试：获取所有用户")
    void testGetAllUsers_Integration() throws Exception {
        // 先创建几个用户
        User user1 = new User(null, "用户 1", "user1@example.com", 20);
        User user2 = new User(null, "用户 2", "user2@example.com", 25);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1)));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2)));

        // 获取所有用户
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * 测试更新用户
     */
    @Test
    @DisplayName("集成测试：更新用户信息")
    void testUpdateUser_Integration() throws Exception {
        // 先创建一个用户
        User newUser = new User(null, "原始用户", "original@example.com", 30);
        
        String createResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper.readTree(createResponse).get("id").asLong();

        // 更新用户信息
        User updatedUser = new User(null, "更新后的用户", "updated@example.com", 35);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("更新后的用户"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.age").value(35));

        // 验证更新后的数据
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("更新后的用户"));
    }

    /**
     * 测试删除用户
     */
    @Test
    @DisplayName("集成测试：删除用户")
    void testDeleteUser_Integration() throws Exception {
        // 先创建一个用户
        User newUser = new User(null, "待删除用户", "delete@example.com", 40);
        
        String createResponse = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long userId = objectMapper.readTree(createResponse).get("id").asLong();

        // 删除用户
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        // 验证用户已被删除
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    /**
     * 测试根据用户名查询用户
     */
    @Test
    @DisplayName("集成测试：根据用户名查询")
    void testGetUserByUsername_Integration() throws Exception {
        // 先创建一个用户
        User newUser = new User(null, "特定用户名", "specific@example.com", 33);
        
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated());

        // 根据用户名查询
        mockMvc.perform(get("/api/users/username/{username}", "特定用户名"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("特定用户名"))
                .andExpect(jsonPath("$.email").value("specific@example.com"));
    }

    /**
     * 测试查询不存在的用户
     */
    @Test
    @DisplayName("集成测试：查询不存在的用户")
    void testGetNonExistentUser_Integration() throws Exception {
        // 查询一个不存在的用户 ID
        mockMvc.perform(get("/api/users/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    /**
     * 测试按年龄范围查询用户
     */
    @Test
    @DisplayName("集成测试：按年龄范围查询")
    void testGetUsersByAgeRange_Integration() throws Exception {
        // 创建不同年龄的用户
        User user1 = new User(null, "年轻用户 1", "young1@example.com", 22);
        User user2 = new User(null, "年轻用户 2", "young2@example.com", 26);
        User user3 = new User(null, "年长用户", "old@example.com", 35);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user1)));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user2)));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user3)));

        // 查询年龄在 20-30 之间的用户
        mockMvc.perform(get("/api/users/age-range")
                        .param("minAge", "20")
                        .param("maxAge", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));  // 应该有 2 个用户符合
    }
}
