package com.qq.ijay997.service.impl;

import com.qq.ijay997.dto.MaskingUserExcel;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.excel.MaskingExcelWriter;
import com.qq.ijay997.service.MaskingExportService;
import com.qq.ijay997.service.UserService;
import com.qq.ijay997.support.DemoMockData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 脱敏演示导出实现。
 * <p>
 * 这里直接写原始值：脱敏由 {@code SensitiveExcelWriteHandler} 在单元格级完成，
 * 源对象不会被改写，同一份数据可安全复用。
 *
 * @author ijay997
 */
@Service
public class MaskingExportServiceImpl implements MaskingExportService {

    private static final String SHEET_NAME = "用户列表";

    @Autowired
    private UserService userService;

    @Override
    public void exportUsers(OutputStream outputStream) {
        List<MaskingUserExcel> rows = userService.findAll().stream()
                .map(this::toRow)
                .collect(Collectors.toList());

        MaskingExcelWriter.write(outputStream, MaskingUserExcel.class, rows, SHEET_NAME);
    }

    private MaskingUserExcel toRow(User user) {
        return MaskingUserExcel.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(DemoMockData.phoneOf(user.getId()))
                .age(user.getAge())
                .build();
    }
}
