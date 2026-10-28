package com.qq.ijay997.controller;

import com.alibaba.excel.EasyExcel;
import com.qq.ijay997.dto.MaskingUserExcel;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.service.UserService;
import com.qq.ijay997.service.impl.MaskingExportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 脱敏演示接口测试：通过请求头模拟不同角色，验证 JSON 响应与 Excel 导出的脱敏效果。
 *
 * @author ijay997
 */
@WebMvcTest(MaskingDemoController.class)
@Import(MaskingExportServiceImpl.class)
class MaskingDemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("默认（无请求头）：按 GUEST 处理，敏感字段全部脱敏")
    void shouldMaskByDefault() throws Exception {
        givenUsers();

        mockMvc.perform(get("/api/demo/masking/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("张*"))
                .andExpect(jsonPath("$.data[0].email").value("z***@example.com"))
                .andExpect(jsonPath("$.data[0].phone").value("138****0001"))
                .andExpect(jsonPath("$.data[0].age").value(25));
    }

    @Test
    @DisplayName("X-Role=ADMIN：敏感字段输出明文")
    void shouldReturnPlainTextForAdmin() throws Exception {
        givenUsers();

        mockMvc.perform(get("/api/demo/masking/users").header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("张三"))
                .andExpect(jsonPath("$.data[0].email").value("zhangsan@example.com"))
                .andExpect(jsonPath("$.data[0].phone").value("13800000001"));
    }

    @Test
    @DisplayName("X-Role=OPERATOR：仅用户名明文，邮箱脱敏")
    void shouldMaskPartiallyForOperator() throws Exception {
        givenUsers();

        mockMvc.perform(get("/api/demo/masking/users").header("X-Role", "OPERATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("张三"))
                .andExpect(jsonPath("$.data[0].email").value("z***@example.com"));
    }

    @Test
    @DisplayName("X-Permissions：显式权限码覆盖角色预置权限")
    void shouldOverridePermissionsByHeader() throws Exception {
        givenUsers();

        mockMvc.perform(get("/api/demo/masking/users")
                        .header("X-Role", "ADMIN")
                        .header("X-Permissions", "user:email:read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("张*"))
                .andExpect(jsonPath("$.data[0].email").value("zhangsan@example.com"));
    }

    @Test
    @DisplayName("导出：默认按 GUEST 处理，Excel 三个敏感单元格全部脱敏")
    void shouldMaskExportedExcelByDefault() throws Exception {
        givenUsers();

        MvcResult result = mockMvc.perform(get("/api/demo/masking/users/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn();

        List<MaskingUserExcel> rows = readRows(result);
        assertEquals(2, rows.size());
        MaskingUserExcel first = rows.get(0);
        assertAll(
                () -> assertEquals("张*", first.getUsername()),
                () -> assertEquals("z***@example.com", first.getEmail()),
                () -> assertEquals("138****0001", first.getPhone()),
                () -> assertEquals(25, first.getAge())
        );
    }

    @Test
    @DisplayName("导出：X-Role=ADMIN 时 Excel 输出明文")
    void shouldExportPlainTextForAdmin() throws Exception {
        givenUsers();

        MvcResult result = mockMvc.perform(get("/api/demo/masking/users/export")
                        .header("X-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andReturn();

        MaskingUserExcel first = readRows(result).get(0);
        assertAll(
                () -> assertEquals("张三", first.getUsername()),
                () -> assertEquals("zhangsan@example.com", first.getEmail()),
                () -> assertEquals("13800000001", first.getPhone())
        );
    }

    @Test
    @DisplayName("导出：X-Permissions 覆盖角色后仅邮箱输出明文")
    void shouldApplyOverriddenPermissionToExport() throws Exception {
        givenUsers();

        MvcResult result = mockMvc.perform(get("/api/demo/masking/users/export")
                        .header("X-Role", "GUEST")
                        .header("X-Permissions", "user:email:read"))
                .andExpect(status().isOk())
                .andReturn();

        MaskingUserExcel first = readRows(result).get(0);
        assertAll(
                () -> assertEquals("张*", first.getUsername()),
                () -> assertEquals("zhangsan@example.com", first.getEmail()),
                () -> assertEquals("138****0001", first.getPhone())
        );
    }

    @Test
    @DisplayName("/me：返回 ThreadLocal 中的 mock 用户与权限")
    void shouldExposeCurrentPrincipal() throws Exception {
        mockMvc.perform(get("/api/demo/masking/me")
                        .header("X-User-Id", "2002")
                        .header("X-Role", "OPERATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("2002"))
                .andExpect(jsonPath("$.data.role").value("OPERATOR"))
                .andExpect(jsonPath("$.data.permissions[0]").value("user:name:read"));
    }

    private void givenUsers() {
        given(userService.findAll()).willReturn(Arrays.asList(
                new User(1L, "张三", "zhangsan@example.com", 25),
                new User(2L, "李四", "lisi@example.com", 30)));
    }

    private List<MaskingUserExcel> readRows(MvcResult result) {
        return EasyExcel.read(new ByteArrayInputStream(
                        result.getResponse().getContentAsByteArray()))
                .head(MaskingUserExcel.class)
                .sheet(0)
                .doReadSync();
    }
}
