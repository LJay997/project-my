package com.qq.ijay997.controller;

import com.qq.ijay997.dto.ExcelImportResult;
import com.qq.ijay997.service.ExcelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ExcelController 接口测试。
 *
 * @author ijay997
 */
@WebMvcTest(ExcelController.class)
class ExcelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExcelService excelService;

    @Test
    @DisplayName("导出接口：返回 xlsx 内容类型与下载响应头")
    void shouldExportUsers() throws Exception {
        doAnswer(invocation -> {
            // 模拟往输出流写一个字节，验证 Controller 正确透传 OutputStream
            invocation.<java.io.OutputStream>getArgument(0).write("xlsx-content".getBytes());
            return null;
        }).when(excelService).exportUsers(any());

        mockMvc.perform(get("/api/excel/users/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes("xlsx-content".getBytes()));
    }

    @Test
    @DisplayName("模板接口：返回下载响应头")
    void shouldDownloadTemplate() throws Exception {
        mockMvc.perform(get("/api/excel/users/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));

        verify(excelService).exportImportTemplate(any());
    }

    @Test
    @DisplayName("导入接口：上传合法文件返回导入结果")
    void shouldImportUsers() throws Exception {
        ExcelImportResult result = new ExcelImportResult();
        result.setTotal(2);
        result.incrementSuccess();
        result.addError(3, "邮箱格式不正确");
        given(excelService.importUsers(any())).willReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.xlsx", "application/vnd.ms-excel", "mock".getBytes());

        mockMvc.perform(multipart("/api/excel/users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(1));
    }

    @Test
    @DisplayName("导入接口：非 Excel 后缀直接返回 400")
    void shouldRejectNonExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "users.txt", "text/plain", "mock".getBytes());

        mockMvc.perform(multipart("/api/excel/users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("仅支持 .xlsx 或 .xls 格式的文件"));
    }

    @Test
    @DisplayName("导入接口：空文件返回 400")
    void shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.xlsx", "application/vnd.ms-excel", new byte[0]);

        mockMvc.perform(multipart("/api/excel/users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请选择要上传的 Excel 文件"));
    }
}
