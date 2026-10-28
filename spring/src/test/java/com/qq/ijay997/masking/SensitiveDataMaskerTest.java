package com.qq.ijay997.masking;

import com.qq.ijay997.dto.UserExcel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文件导出场景脱敏测试：反射改写 DTO 的敏感字段。
 *
 * @author ijay997
 */
class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("无权限：用户名与邮箱被改写，非敏感字段保持不变")
    void shouldMaskAllSensitiveFieldsWithoutPermission() {
        UserExcel row = row("张三", "zhangsan@example.com");

        masker.maskBean(row);

        assertAll(
                () -> assertEquals("张*", row.getUsername()),
                () -> assertEquals("z***@example.com", row.getEmail()),
                () -> assertEquals(1L, row.getId()),
                () -> assertEquals(16, row.getAge()),
                () -> assertEquals("青少年", row.getAgeGroup())
        );
    }

    @Test
    @DisplayName("全部权限：敏感字段保持明文")
    void shouldKeepPlainTextWithPermission() {
        UserContextHolder.set(principal("ADMIN", PermissionCodes.ALL));
        UserExcel row = row("张三", "zhangsan@example.com");

        masker.maskBean(row);

        assertAll(
                () -> assertEquals("张三", row.getUsername()),
                () -> assertEquals("zhangsan@example.com", row.getEmail())
        );
    }

    @Test
    @DisplayName("部分权限：仅持有权限的字段保持明文")
    void shouldMaskOnlyFieldsWithoutPermission() {
        UserContextHolder.set(principal("OPERATOR", PermissionCodes.forRole("OPERATOR")));
        UserExcel row = row("张三", "zhangsan@example.com");

        masker.maskBean(row);

        assertAll(
                () -> assertEquals("张三", row.getUsername()),
                () -> assertEquals("z***@example.com", row.getEmail())
        );
    }

    @Test
    @DisplayName("批量处理：集合中每一行都被脱敏")
    void shouldMaskEveryBeanInIterable() {
        List<UserExcel> rows = Arrays.asList(
                row("张三", "zhangsan@example.com"),
                row("李四", "lisi@example.com"));

        masker.maskBeans(rows);

        assertAll(
                () -> assertEquals("张*", rows.get(0).getUsername()),
                () -> assertEquals("李*", rows.get(1).getUsername()),
                () -> assertEquals("z***@example.com", rows.get(0).getEmail()),
                () -> assertEquals("l***@example.com", rows.get(1).getEmail())
        );
    }

    @Test
    @DisplayName("容错：null 集合与 null 元素不抛异常")
    void shouldIgnoreNull() {
        assertDoesNotThrow(() -> masker.maskBeans(null));
        assertDoesNotThrow(() -> masker.maskBeans(Collections.singletonList(null)));
        assertDoesNotThrow(() -> masker.maskBean(null));
    }

    private UserExcel row(String username, String email) {
        return UserExcel.builder()
                .id(1L)
                .username(username)
                .email(email)
                .age(16)
                .ageGroup("青少年")
                .build();
    }

    private UserPrincipal principal(String role, java.util.Set<String> permissions) {
        return new UserPrincipal("1001", "mock-1001", role, permissions);
    }
}
