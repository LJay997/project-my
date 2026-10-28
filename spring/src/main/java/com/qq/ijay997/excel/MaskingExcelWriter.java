package com.qq.ijay997.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;

import java.io.OutputStream;
import java.util.List;

/**
 * 带脱敏能力的 EasyExcel 写出门面。
 * <p>
 * 所有文件导出统一从这里创建写出器，{@link SensitiveExcelWriteHandler} 会自动挂载：
 * 导出模型只要在敏感字段上标注 {@code @Sensitive}，无需编写任何脱敏代码。
 *
 * @author ijay997
 */
public final class MaskingExcelWriter {

    private static final SensitiveExcelWriteHandler HANDLER = new SensitiveExcelWriteHandler();

    private MaskingExcelWriter() {
    }

    /**
     * 单 Sheet 快捷写出。
     *
     * @param out       输出流
     * @param head      导出模型类型
     * @param data      原始数据（无需预先脱敏）
     * @param sheetName Sheet 名称
     */
    public static void write(OutputStream out, Class<?> head, List<?> data, String sheetName) {
        builder(out).sheet(sheetName).head(head).doWrite(data);
    }

    /**
     * 多 Sheet 场景：先 build，再按 Sheet 分别 write。
     *
     * @param out 输出流
     * @return 已注册脱敏处理器的写出器构建器
     */
    public static ExcelWriterBuilder builder(OutputStream out) {
        return EasyExcel.write(out).registerWriteHandler(HANDLER);
    }

    /**
     * 指定表头类型的写出器构建器。
     *
     * @param out  输出流
     * @param head 导出模型类型
     * @return 已注册脱敏处理器的写出器构建器
     */
    public static ExcelWriterBuilder builder(OutputStream out, Class<?> head) {
        return EasyExcel.write(out, head).registerWriteHandler(HANDLER);
    }
}
