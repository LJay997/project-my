package com.qq.ijay997.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Pattern;

/**
 * 用户 Excel 导入模型（同时用作导入模板表头）。
 * <p>
 * 与导出模型分开定义：导入只接受业务必需的三个字段，
 * 避免用户误填用户 ID、派生列等只读数据。
 * 注意：用户名、邮箱在导入监听器中做必填与格式校验。
 *
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class UserImportExcel {

    @ExcelProperty(value = "用户名", index = 0)
    @ColumnWidth(20)
    private String username;

    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "邮箱格式错误")
    @ExcelProperty(value = "邮箱", index = 1)
    @ColumnWidth(30)
    private String email;

    @ExcelProperty(value = "年龄", index = 2)
    @ColumnWidth(10)
    private Integer age;
}
