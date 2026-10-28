package com.qq.ijay997.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.BooleanEnum;
import com.alibaba.excel.enums.poi.FillPatternTypeEnum;
import com.qq.ijay997.masking.PermissionCodes;
import com.qq.ijay997.masking.Sensitive;
import com.qq.ijay997.masking.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏演示导出模型。
 * <p>
 * 与 {@code UserExcel} 相互独立：本模型面向 {@code /api/demo/masking} 演示端点，
 * 额外包含手机号列，用于展示 PHONE 类型的脱敏效果。
 * 字段上的 {@code @Sensitive} 由 {@code SensitiveExcelWriteHandler} 在写出时读取。
 *
 * @author ijay997
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
@HeadFontStyle(bold = BooleanEnum.TRUE, fontHeightInPoints = 12)
@HeadStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 40)
public class MaskingUserExcel {

    @ExcelProperty(value = "用户ID", index = 0)
    @ColumnWidth(10)
    private Long id;

    @ExcelProperty(value = "用户名", index = 1)
    @ColumnWidth(20)
    @Sensitive(type = SensitiveType.NAME, permission = PermissionCodes.USER_NAME_READ)
    private String username;

    @ExcelProperty(value = "邮箱", index = 2)
    @ColumnWidth(30)
    @Sensitive(type = SensitiveType.EMAIL, permission = PermissionCodes.USER_EMAIL_READ)
    private String email;

    @ExcelProperty(value = "手机号", index = 3)
    @ColumnWidth(20)
    @Sensitive(type = SensitiveType.PHONE, permission = PermissionCodes.USER_PHONE_READ)
    private String phone;

    @ExcelProperty(value = "年龄", index = 4)
    @ColumnWidth(10)
    private Integer age;
}
