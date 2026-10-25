package com.qq.ijay997.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户年龄统计 Excel 模型，用于多 Sheet 导出的第二个 Sheet。
 *
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class UserStatisticsExcel {

    @ExcelProperty(value = "年龄段", index = 0)
    @ColumnWidth(15)
    private String ageGroup;

    @ExcelProperty(value = "用户数量", index = 1)
    @ColumnWidth(15)
    private Long count;
}
