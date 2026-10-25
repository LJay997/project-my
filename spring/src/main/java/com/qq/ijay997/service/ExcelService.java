package com.qq.ijay997.service;

import com.qq.ijay997.dto.ExcelImportResult;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Excel 导入导出服务。
 * <p>
 * 使用 OutputStream / InputStream 而非 HttpServletResponse，方便单元测试与复用
 * （Controller 负责 HTTP 头，Service 只负责 Excel 内容）。
 *
 * @author ijay997
 */
public interface ExcelService {

    /**
     * 导出用户列表（含"用户列表"和"年龄统计"两个 Sheet）。
     *
     * @param outputStream 输出流
     */
    void exportUsers(OutputStream outputStream);

    /**
     * 下载用户导入模板（仅表头，无数据）。
     *
     * @param outputStream 输出流
     */
    void exportImportTemplate(OutputStream outputStream);

    /**
     * 导入用户。
     *
     * @param inputStream Excel 输入流
     * @return 导入结果（成功数、失败数、逐行错误）
     */
    ExcelImportResult importUsers(InputStream inputStream);
}
