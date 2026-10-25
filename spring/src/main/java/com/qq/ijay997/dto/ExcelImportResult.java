package com.qq.ijay997.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导入结果：汇总总行数、成功/失败数量以及逐行错误信息。
 *
 * @author ijay997
 */
@Data
public class ExcelImportResult {

    /** 读取到的有效数据总行数 */
    private int total;

    /** 导入成功数量 */
    private int successCount;

    /** 导入失败数量 */
    private int failureCount;

    /** 逐行错误信息，便于前端直接展示 */
    private List<String> errors = new ArrayList<>();

    /**
     * 记录一条行级错误。
     *
     * @param rowNumber Excel 行号（从 1 开始，表头为第 1 行）
     * @param message   错误原因
     */
    public void addError(int rowNumber, String message) {
        this.failureCount++;
        this.errors.add("第 " + rowNumber + " 行：" + message);
    }

    /** 标记一行导入成功。 */
    public void incrementSuccess() {
        this.successCount++;
    }

    /** 便于前端只读访问错误列表。 */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
