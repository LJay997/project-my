package com.qq.ijay997.controller;

import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.auth.UserContextHolder;
import com.qq.ijay997.auth.UserPrincipal;
import com.qq.ijay997.service.MaskingExportService;
import com.qq.ijay997.service.UserService;
import com.qq.ijay997.support.DemoMockData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据脱敏演示接口。
 * <p>
 * 通过请求头 mock 权限（详见 {@code MockUserContextFilter}）：
 * <pre>
 *   GET /api/demo/masking/users         JSON 响应脱敏
 *   GET /api/demo/masking/users/export  Excel 导出脱敏
 *   GET /api/demo/masking/me            查看当前 mock 用户与权限
 * </pre>
 *
 * @author ijay997
 */
@RestController
@RequestMapping("/api/demo/masking")
public class MaskingDemoController {

    @Autowired
    private UserService userService;

    @Autowired
    private MaskingExportService maskingExportService;

    /**
     * 返回用户列表：{@link UserDTO} 上的 {@code @Sensitive} 字段由 Jackson 序列化器按权限脱敏。
     */
    @GetMapping("/users")
    public Result<List<UserDTO>> users() {
        List<UserDTO> users = userService.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    /**
     * 导出用户列表：文件内容由 {@code SensitiveExcelWriteHandler} 按与 JSON 相同的权限规则脱敏。
     */
    @GetMapping("/users/export")
    public void exportUsers(HttpServletResponse response) throws IOException {
        prepareDownload(response, "用户列表-脱敏演示.xlsx");
        maskingExportService.exportUsers(response.getOutputStream());
    }

    /**
     * 返回当前 ThreadLocal 中的 mock 用户与权限，便于观察上下文是否生效。
     */
    @GetMapping("/me")
    public Result<UserPrincipal> me() {
        return Result.success(UserContextHolder.get());
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getUsername(), user.getEmail(),
                DemoMockData.phoneOf(user.getId()), user.getAge(), null, null);
    }

    private void prepareDownload(HttpServletResponse response, String fileName) throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replace("+", "%20");
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded);
    }
}
