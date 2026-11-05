package com.qq.ijay997.service;

import com.alibaba.excel.EasyExcel;
import com.qq.ijay997.dto.MaskingUserExcel;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.auth.PermissionCodes;
import com.qq.ijay997.auth.UserContextHolder;
import com.qq.ijay997.auth.UserPrincipal;
import com.qq.ijay997.service.impl.MaskingExportServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 脱敏导出服务测试：直接验证 EasyExcel 写出产物中的单元格值。
 * <p>
 * 脱敏由 {@code SensitiveExcelWriteHandler} 完成，本测试同时验证"源对象不被改写"。
 *
 * @author ijay997
 */
@ExtendWith(MockitoExtension.class)
class MaskingExportServiceTest {

    @Mock
    private UserService userService;

    private final MaskingExportServiceImpl exportService = new MaskingExportServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exportService, "userService", userService);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("导出：无上下文时按无权限处理，用户名/邮箱/手机号全部脱敏")
    void shouldMaskAllSensitiveColumnsWithoutContext() {
        when(userService.findAll()).thenReturn(sampleUsers());

        List<MaskingUserExcel> rows = export();

        assertEquals(2, rows.size());
        MaskingUserExcel first = rows.get(0);
        assertAll(
                () -> assertEquals(1L, first.getId()),
                () -> assertEquals("张*", first.getUsername()),
                () -> assertEquals("z***@example.com", first.getEmail()),
                () -> assertEquals("138****0001", first.getPhone()),
                () -> assertEquals(25, first.getAge())
        );
    }

    @Test
    @DisplayName("导出：ADMIN 拥有全部权限，三项敏感字段输出明文")
    void shouldExportPlainTextForAdmin() {
        UserContextHolder.set(principal(PermissionCodes.ALL));
        when(userService.findAll()).thenReturn(sampleUsers());

        List<MaskingUserExcel> rows = export();

        MaskingUserExcel first = rows.get(0);
        assertAll(
                () -> assertEquals("张三", first.getUsername()),
                () -> assertEquals("zhangsan@example.com", first.getEmail()),
                () -> assertEquals("13800000001", first.getPhone())
        );
    }

    @Test
    @DisplayName("导出：OPERATOR 仅持有用户名权限，邮箱与手机号脱敏")
    void shouldMaskEmailAndPhoneForOperator() {
        UserContextHolder.set(principal(PermissionCodes.forRole("OPERATOR")));
        when(userService.findAll()).thenReturn(sampleUsers());

        List<MaskingUserExcel> rows = export();

        MaskingUserExcel first = rows.get(0);
        assertAll(
                () -> assertEquals("张三", first.getUsername()),
                () -> assertEquals("z***@example.com", first.getEmail()),
                () -> assertEquals("138****0001", first.getPhone())
        );
    }

    @Test
    @DisplayName("导出：脱敏只作用于单元格，不修改源实体")
    void shouldNotMutateSourceEntities() {
        List<User> users = sampleUsers();
        when(userService.findAll()).thenReturn(users);

        export();

        User first = users.get(0);
        assertAll(
                () -> assertEquals("张三", first.getUsername()),
                () -> assertEquals("zhangsan@example.com", first.getEmail())
        );
    }

    private List<MaskingUserExcel> export() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        exportService.exportUsers(output);

        return EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .head(MaskingUserExcel.class)
                .sheet(0)
                .doReadSync();
    }

    private List<User> sampleUsers() {
        return Arrays.asList(
                new User(1L, "张三", "zhangsan@example.com", 25),
                new User(2L, "李四", "lisi@example.com", 30));
    }

    private UserPrincipal principal(java.util.Set<String> permissions) {
        return new UserPrincipal("1001", "mock-1001", "ADMIN", permissions);
    }
}
