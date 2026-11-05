package com.qq.ijay997.auth;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * mock 用户上下文过滤器（身份适配层）。
 * <p>
 * 通过请求头模拟登录用户与权限码，写入 {@link UserContextHolder}，请求结束后必定清理：
 * <ul>
 *   <li>{@code X-User-Id}：用户标识，默认 1001；</li>
 *   <li>{@code X-Role}：ADMIN / OPERATOR / GUEST，默认 GUEST；</li>
 *   <li>{@code X-Permissions}：逗号分隔的权限码，存在时覆盖角色预置权限；</li>
 *   <li>{@code X-Level}：数据权限等级阈值，非负整数。缺省、空白、非数字或负数一律视为未设置
 *       （{@code null}），拦截器不追加任何条件，绝不因脏 header 返回 500。</li>
 * </ul>
 * 默认 GUEST（空权限）配合"无权限即脱敏"，保证不携带请求头时不会泄露敏感数据。
 * <p>
 * 本类只负责"把 HTTP 请求转换为身份上下文"，属于身份基础设施；具体的脱敏规则仍留在
 * {@code com.qq.ijay997.masking}。<b>未携带合法 {@code X-Level} 时</b>不设置 {@code level}，
 * 因此数据权限拦截器保持休眠，行为与引入该请求头之前完全一致。
 *
 * @author ijay997
 */
@Component
@Order(2)
public class MockUserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ROLE = "X-Role";
    private static final String HEADER_PERMISSIONS = "X-Permissions";
    private static final String HEADER_LEVEL = "X-Level";

    private static final String DEFAULT_USER_ID = "1001";
    private static final String DEFAULT_ROLE = "GUEST";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            UserContextHolder.set(buildPrincipal(request));
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private UserPrincipal buildPrincipal(HttpServletRequest request) {
        String userId = headerOrDefault(request, HEADER_USER_ID, DEFAULT_USER_ID);
        String role = headerOrDefault(request, HEADER_ROLE, DEFAULT_ROLE).toUpperCase();

        Set<String> permissions = resolvePermissions(request, role);
        Integer level = resolveLevel(request);
        return new UserPrincipal(userId, "mock-" + userId, role, permissions, level);
    }

    /**
     * 解析 {@code X-Level}。缺省、空白、非数字、负数一律返回 {@code null}（视为未设置），
     * 保证脏请求头不会导致 500，也不会误加数据权限条件。
     */
    private Integer resolveLevel(HttpServletRequest request) {
        String header = request.getHeader(HEADER_LEVEL);
        if (header == null || header.trim().isEmpty()) {
            return null;
        }
        try {
            int level = Integer.parseInt(header.trim());
            return level < 0 ? null : level;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Set<String> resolvePermissions(HttpServletRequest request, String role) {
        String header = request.getHeader(HEADER_PERMISSIONS);
        if (header == null || header.trim().isEmpty()) {
            return PermissionCodes.forRole(role);
        }
        Set<String> permissions = Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return permissions;
    }

    private String headerOrDefault(HttpServletRequest request, String header, String defaultValue) {
        String value = request.getHeader(header);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
