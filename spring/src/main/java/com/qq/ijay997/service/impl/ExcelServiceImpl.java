package com.qq.ijay997.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.qq.ijay997.dto.ExcelImportResult;
import com.qq.ijay997.dto.UserExcel;
import com.qq.ijay997.dto.UserImportExcel;
import com.qq.ijay997.dto.UserStatisticsExcel;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.excel.UserExcelImportListener;
import com.qq.ijay997.service.ExcelService;
import com.qq.ijay997.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入导出实现（EasyExcel）。
 *
 * @author ijay997
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    private static final String USER_SHEET_NAME = "用户列表";
    private static final String STATISTICS_SHEET_NAME = "年龄统计";

    @Autowired
    private UserService userService;

    @Autowired
    private Validator validator;

    @Override
    public void exportUsers(OutputStream outputStream) {
        List<User> users = userService.findAll();

        // Sheet 1：用户列表
        List<UserExcel> rows = new ArrayList<>(users.size());
        for (User user : users) {
            rows.add(UserExcel.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .age(user.getAge())
                    .ageGroup(toAgeGroup(user.getAge()))
                    .build());
        }

        // Sheet 2：按年龄段统计
        List<UserStatisticsExcel> statistics = buildStatistics(rows);

        // 多个 Sheet 共用一次写入会话，避免多次打开文件
        ExcelWriter writer = EasyExcel.write(outputStream).build();
        try {
            WriteSheet userSheet = EasyExcel.writerSheet(0, USER_SHEET_NAME)
                    .head(UserExcel.class)
                    .build();
            WriteSheet statisticsSheet = EasyExcel.writerSheet(1, STATISTICS_SHEET_NAME)
                    .head(UserStatisticsExcel.class)
                    .build();

            writer.write(rows, userSheet);
            writer.write(statistics, statisticsSheet);
        } finally {
            // finish() 会把数据刷入输出流并关闭文件
            writer.finish();
        }
    }

    @Override
    public void exportImportTemplate(OutputStream outputStream) {
        // 只写表头：doWrite 空集合适用于模板下载
        EasyExcel.write(outputStream, UserImportExcel.class)
                .sheet("用户导入模板")
                .doWrite(Collections.emptyList());
    }

    @Override
    public ExcelImportResult importUsers(InputStream inputStream) {
        ExcelImportResult result = new ExcelImportResult();
        UserExcelImportListener listener = new UserExcelImportListener(userService, validator, result);

        // 默认读取第一个 Sheet，表头占 1 行；监听器逐行回调，内存友好
        EasyExcel.read(inputStream, UserImportExcel.class, listener)
                .sheet()
                .doRead();

        return result;
    }

    /**
     * 按年龄段分组统计。
     */
    private List<UserStatisticsExcel> buildStatistics(List<UserExcel> rows) {
        Map<String, Long> counter = new LinkedHashMap<>();
        for (UserExcel row : rows) {
            counter.merge(row.getAgeGroup(), 1L, Long::sum);
        }

        List<UserStatisticsExcel> statistics = new ArrayList<>(counter.size());
        counter.forEach((ageGroup, count) ->
                statistics.add(new UserStatisticsExcel(ageGroup, count)));
        return statistics;
    }

    /**
     * 年龄 -&gt; 年龄段（与实体年龄字段保持一致的分组口径）。
     */
    private String toAgeGroup(Integer age) {
        if (age == null) {
            return "未知";
        }
        if (age < 18) {
            return "青少年";
        }
        return age < 60 ? "成年" : "老年";
    }
}
