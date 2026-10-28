package com.qq.ijay997.masking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JSON 场景脱敏测试：直接验证 Jackson 序列化产物。
 *
 * @author ijay997
 */
class SensitiveJsonSerializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("无上下文：敏感字段全部脱敏（fail-closed）")
    void shouldMaskWhenNoContext() throws Exception {
        String json = objectMapper.writeValueAsString(user());
        JsonNode node = objectMapper.readTree(json);

        assertAll(
                () -> assertEquals("张*", node.get("username").asText()),
                () -> assertEquals("z***@example.com", node.get("email").asText()),
                () -> assertEquals("138****5678", node.get("phone").asText()),
                () -> assertEquals(1L, node.get("id").asLong())
        );
    }

    @Test
    @DisplayName("ADMIN 权限：敏感字段输出明文")
    void shouldKeepPlainTextWithPermission() throws Exception {
        UserContextHolder.set(principal("ADMIN", PermissionCodes.ALL));

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(user()));

        assertAll(
                () -> assertEquals("张三", node.get("username").asText()),
                () -> assertEquals("zhangsan@example.com", node.get("email").asText()),
                () -> assertEquals("13812345678", node.get("phone").asText())
        );
    }

    @Test
    @DisplayName("OPERATOR 权限：仅用户名明文，邮箱与手机号脱敏")
    void shouldMaskPartiallyByPermissionCode() throws Exception {
        UserContextHolder.set(principal("OPERATOR", PermissionCodes.forRole("OPERATOR")));

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(user()));

        assertAll(
                () -> assertEquals("张三", node.get("username").asText()),
                () -> assertEquals("z***@example.com", node.get("email").asText()),
                () -> assertEquals("138****5678", node.get("phone").asText())
        );
    }

    @Test
    @DisplayName("集合与包装类型：列表元素中的敏感字段同样按权限脱敏")
    void shouldMaskInsideCollection() throws Exception {
        UserContextHolder.set(principal("GUEST", PermissionCodes.NONE));

        JsonNode data = objectMapper.readTree(
                objectMapper.writeValueAsString(Result.success(Collections.singletonList(user()))))
                .get("data").get(0);

        assertAll(
                () -> assertEquals("张*", data.get("username").asText()),
                () -> assertEquals("z***@example.com", data.get("email").asText())
        );
    }

    @Test
    @DisplayName("null 敏感字段：输出 null，不参与脱敏")
    void shouldWriteNull() throws Exception {
        UserDTO dto = user();
        dto.setPhone(null);

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertTrue(node.get("phone").isNull());
        assertFalse(node.get("email").isNull());
    }

    private UserDTO user() {
        UserDTO dto = new UserDTO();
        dto.setId(1L);
        dto.setUsername("张三");
        dto.setEmail("zhangsan@example.com");
        dto.setPhone("13812345678");
        return dto;
    }

    private UserPrincipal principal(String role, java.util.Set<String> permissions) {
        return new UserPrincipal("1001", "mock-1001", role, permissions);
    }
}
