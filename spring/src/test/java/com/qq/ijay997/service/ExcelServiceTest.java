package com.qq.ijay997.service;

import com.alibaba.excel.EasyExcel;
import com.qq.ijay997.dto.ExcelImportResult;
import com.qq.ijay997.dto.UserExcel;
import com.qq.ijay997.dto.UserImportExcel;
import com.qq.ijay997.dto.UserStatisticsExcel;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.auth.PermissionCodes;
import com.qq.ijay997.auth.UserContextHolder;
import com.qq.ijay997.auth.UserPrincipal;
import com.qq.ijay997.service.impl.ExcelServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.Validation;
import javax.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExcelService 单元测试：直接验证 EasyExcel 读写的真实产物。
 * <p>
 * 服务实现使用字段注入，这里手工装配真实的 {@link Validator}，
 * 以便同时覆盖导出脱敏与导入校验两条链路。
 *
 * @author ijay997
 */
@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    @Mock
    private UserService userService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final ExcelServiceImpl excelService = new ExcelServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(excelService, "userService", userService);
        ReflectionTestUtils.setField(excelService, "validator", validator);
    }

    @AfterEach
    void tearDown() {
        // ThreadLocal 必须清理，避免测试线程复用时权限串号
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("导出：生成包含两个 Sheet 的 xlsx，数据与统计正确")
    void shouldExportUsersWithTwoSheets() {
        // given：具备全部敏感数据权限，导出结果为明文
        UserContextHolder.set(principal(PermissionCodes.ALL));
        List<User> users = Arrays.asList(
                new User(1L, "张三", "zhangsan@example.com", 16),
                new User(2L, "李四", "lisi@example.com", 30),
                new User(3L, "王五", "wangwu@example.com", 65),
                new User(4L, "赵六", "zhaoliu@example.com", null)
        );
        when(userService.findAll()).thenReturn(users);

        // when
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelService.exportUsers(output);
        byte[] bytes = output.toByteArray();

        // then：Sheet1 用户列表
        List<UserExcel> userRows = EasyExcel.read(new ByteArrayInputStream(bytes))
                .head(UserExcel.class)
                .sheet(0)
                .doReadSync();

        assertEquals(4, userRows.size());
        assertAll(
                () -> assertEquals(1L, userRows.get(0).getId()),
                () -> assertEquals("张三", userRows.get(0).getUsername()),
                () -> assertEquals("zhangsan@example.com", userRows.get(0).getEmail()),
                () -> assertEquals(16, userRows.get(0).getAge()),
                () -> assertEquals("青少年", userRows.get(0).getAgeGroup()),
                () -> assertEquals("成年", userRows.get(1).getAgeGroup()),
                () -> assertEquals("老年", userRows.get(2).getAgeGroup()),
                () -> assertEquals("未知", userRows.get(3).getAgeGroup())
        );

        // Sheet2 年龄统计：四个年龄段各 1 人
        List<UserStatisticsExcel> statistics = EasyExcel.read(new ByteArrayInputStream(bytes))
                .head(UserStatisticsExcel.class)
                .sheet(1)
                .doReadSync();

        assertEquals(4, statistics.size());
        assertTrue(statistics.stream().allMatch(item -> item.getCount() == 1L));
    }

    @Test
    @DisplayName("导出：无敏感权限时用户名与邮箱写出脱敏值")
    void shouldMaskSensitiveColumnsWithoutPermission() {
        // given：未设置上下文，按匿名用户（无权限）处理
        List<User> users = Collections.singletonList(
                new User(1L, "张三", "zhangsan@example.com", 16));
        when(userService.findAll()).thenReturn(users);

        // when
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelService.exportUsers(output);

        List<UserExcel> rows = EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .head(UserExcel.class)
                .sheet(0)
                .doReadSync();

        // then
        assertEquals(1, rows.size());
        assertAll(
                () -> assertEquals("张*", rows.get(0).getUsername()),
                () -> assertEquals("z***@example.com", rows.get(0).getEmail()),
                () -> assertEquals("青少年", rows.get(0).getAgeGroup())
        );
    }

    @Test
    @DisplayName("模板：只输出表头，不含数据行")
    void shouldExportTemplateWithHeadersOnly() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelService.exportImportTemplate(output);

        List<UserImportExcel> rows = EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .head(UserImportExcel.class)
                .sheet(0)
                .doReadSync();

        assertTrue(rows.isEmpty(), "导入模板不应包含数据行");
    }

    @Test
    @DisplayName("导入：合法行入库成功，非法行返回行级错误")
    void shouldImportAndCollectRowErrors() {
        // given：4 行数据，其中 3 行非法
        List<UserImportExcel> rows = Arrays.asList(
                new UserImportExcel("张三", "zhangsan@example.com", 25),
                new UserImportExcel("  ", "blank@example.com", 20),
                new UserImportExcel("李四", "not-an-email", 30),
                new UserImportExcel("王五", "wangwu@example.com", -1)
        );
        byte[] excelBytes = toExcelBytes(rows);
        when(userService.importUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ExcelImportResult result = excelService.importUsers(new ByteArrayInputStream(excelBytes));

        // then
        assertAll(
                () -> assertEquals(4, result.getTotal()),
                () -> assertEquals(1, result.getSuccessCount()),
                () -> assertEquals(3, result.getFailureCount()),
                () -> assertEquals(3, result.getErrors().size())
        );
        assertTrue(result.getErrors().get(0).contains("用户名不能为空"));
        assertTrue(result.getErrors().get(1).contains("邮箱格式错误"));
        assertTrue(result.getErrors().get(2).contains("年龄不能为负数"));
        verify(userService, times(1)).importUser(any(User.class));
    }

    @Test
    @DisplayName("导入：单元格类型无法转换时记录错误并继续读取后续行")
    void shouldRecordConvertErrorAndContinue() {
        // 直接用原始行写入，让"年龄"列出现文本，触发 ExcelDataConvertException
        List<List<String>> head = Arrays.asList(
                Collections.singletonList("用户名"),
                Collections.singletonList("邮箱"),
                Collections.singletonList("年龄")
        );
        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("张三", "zhangsan@example.com", "不是数字"));
        data.add(Arrays.asList("李四", "lisi@example.com", "30"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).head(head).sheet("用户导入模板").doWrite(data);

        when(userService.importUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExcelImportResult result = excelService.importUsers(
                new ByteArrayInputStream(output.toByteArray()));

        assertAll(
                () -> assertEquals(2, result.getTotal()),
                () -> assertEquals(1, result.getSuccessCount()),
                () -> assertEquals(1, result.getFailureCount()),
                () -> assertTrue(result.getErrors().get(0).contains("数据格式无法转换"),
                        "应提示具体单元格类型转换失败")
        );
        verify(userService, times(1)).importUser(any(User.class));
    }

    /**
     * 用导入模型生成内存中的 xlsx 文件。
     */
    private byte[] toExcelBytes(List<UserImportExcel> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, UserImportExcel.class)
                .sheet("用户导入模板")
                .doWrite(rows);
        return output.toByteArray();
    }

    private UserPrincipal principal(java.util.Set<String> permissions) {
        return new UserPrincipal("1001", "mock-1001", "ADMIN", permissions);
    }
}
