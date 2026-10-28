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
 * 用户 Excel 导出模型。
 * <p>
 * {@code index} 显式指定列顺序；{@code @ExcelIgnoreUnannotated} 保证未标注字段不会被导出。
 * 表头样式通过注解统一声明，避免在业务代码中写样式模板。
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
public class UserExcel {

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

    @ExcelProperty(value = "年龄", index = 3)
    @ColumnWidth(10)
    private Integer age;

    /**
     * 派生列：由服务层按年龄计算，仅用于导出，不参与持久化。
     */
    @ExcelProperty(value = "年龄段", index = 4)
    @ColumnWidth(12)
    private String ageGroup;
}
