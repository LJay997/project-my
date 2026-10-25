package com.qq.ijay997.controller;

import com.alibaba.excel.exception.ExcelAnalysisException;
import com.qq.ijay997.dto.ExcelImportResult;
import com.qq.ijay997.dto.Result;
import com.qq.ijay997.service.ExcelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * EasyExcel 使用示例接口。
 *
 * <pre>
 *  导出用户列表（多 Sheet）：GET  /api/excel/users/export
 *  下载导入模板：            GET  /api/excel/users/template
 *  导入用户：                POST /api/excel/users/import   (multipart/form-data, 字段名 file)
 * </pre>
 *
 * @author ijay997
 */
@Slf4j
@RestController
@RequestMapping("/api/excel/users")
public class ExcelController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private ExcelService excelService;

    /**
     * 导出用户列表：一个文件包含"用户列表"与"年龄统计"两个 Sheet。
     *
     * @param response HTTP 响应
     */
    @GetMapping("/export")
    public void exportUsers(HttpServletResponse response) throws IOException {
        prepareDownload(response, "用户列表.xlsx");
        excelService.exportUsers(response.getOutputStream());
    }

    /**
     * 下载用户导入模板（仅表头）。
     *
     * @param response HTTP 响应
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        prepareDownload(response, "用户导入模板.xlsx");
        excelService.exportImportTemplate(response.getOutputStream());
    }

    /**
     * 导入用户。上传文件字段名必须为 {@code file}。
     *
     * @param file 上传的 Excel 文件（.xlsx / .xls）
     * @return 导入结果（成功数、失败数、行级错误）
     */
    @PostMapping("/import")
    public Result<ExcelImportResult> importUsers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的 Excel 文件");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null
                || (!fileName.toLowerCase().endsWith(".xlsx")
                && !fileName.toLowerCase().endsWith(".xls"))) {
            return Result.error(400, "仅支持 .xlsx 或 .xls 格式的文件");
        }

        try (InputStream inputStream = file.getInputStream()) {
            ExcelImportResult result = excelService.importUsers(inputStream);
            return Result.success(result);
        } catch (IOException e) {
            log.error("读取上传文件失败", e);
            return Result.error(500, "读取上传文件失败：" + e.getMessage());
        } catch (ExcelAnalysisException e) {
            log.warn("Excel 解析失败：{}", e.getMessage());
            return Result.error(400, "Excel 解析失败，请检查文件内容与模板是否一致：" + e.getMessage());
        }
    }

    /**
     * 设置下载响应头。文件名使用 RFC 5987 的 {@code filename*=UTF-8''} 形式，避免中文乱码。
     */
    private void prepareDownload(HttpServletResponse response, String fileName) throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded);
    }
}
