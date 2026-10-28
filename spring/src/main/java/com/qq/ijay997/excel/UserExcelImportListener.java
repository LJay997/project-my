package com.qq.ijay997.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.qq.ijay997.dto.ExcelImportResult;
import com.qq.ijay997.dto.UserImportExcel;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.service.UserService;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户 Excel 导入监听器。
 * <p>
 * EasyExcel 采用 SAX 逐行回调，`invoke` 中一行一行处理，内存占用与文件大小无关。
 * 这里做三件事：行级校验、逐行入库（单行失败不影响其他行）、收集错误信息。
 *
 * @author ijay997
 */
@Slf4j
public class UserExcelImportListener extends AnalysisEventListener<UserImportExcel> {

    /** 简单邮箱格式校验，生产环境可用 Apache Commons Validator 替代 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserService userService;

    private final Validator validator;

    private final ExcelImportResult result;

    public UserExcelImportListener(UserService userService, Validator validator, ExcelImportResult result) {
        this.userService = userService;
        this.validator = validator;
        this.result = result;
    }



    @Override
    public void invoke(UserImportExcel data, AnalysisContext context) {
        // Excel 行号从 1 开始，表头占第 1 行，因此数据行号 = 索引 + 1
        int rowNumber = context.readRowHolder().getRowIndex() + 1;
        result.setTotal(result.getTotal() + 1);

        String username = trimToNull(data.getUsername());
        String email = trimToNull(data.getEmail());
        Integer age = data.getAge();

        Set<ConstraintViolation<UserImportExcel>> validate = validator.validate(data);
        String errorMessages = validate.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(","));
        if (!errorMessages.isEmpty()){
            result.addError(rowNumber, String.format("第 %s 行数据错误 %s", rowNumber, errorMessages));
            // 校验不通过的行直接跳过，避免脏数据入库
            return;
        }

        // 2. 入库：单行 try/catch，保证一条脏数据不阻断整个文件
        //    文件导入为一类独立业务场景，走 importUser 以便产生 IMPORT 类型的远端消息
        try {
            userService.importUser(new User(null, username, email, age));
            result.incrementSuccess();
        } catch (Exception e) {
            log.warn("导入第 {} 行失败：{}", rowNumber, e.getMessage());
            result.addError(rowNumber, "入库失败：" + e.getMessage());
        }
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        // 类型转换异常（例如年龄列填了文本）可定位到具体单元格，记录后继续读取后续行
        if (exception instanceof ExcelDataConvertException) {
            ExcelDataConvertException convertException = (ExcelDataConvertException) exception;
            int rowIndex = convertException.getRowIndex() + 1;
            int columnIndex = convertException.getColumnIndex() + 1;
            // 该行未进入 invoke()，需在这里补计总行数
            result.setTotal(result.getTotal() + 1);
            result.addError(rowIndex, "第 " + columnIndex + " 列数据格式无法转换："
                    + convertException.getCellData().getStringValue());
            return;
        }
        throw exception;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("Excel 导入完成，总计 {} 行，成功 {} 行，失败 {} 行",
                result.getTotal(), result.getSuccessCount(), result.getFailureCount());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}