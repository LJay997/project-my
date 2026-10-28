package com.qq.ijay997.config;

import com.qq.ijay997.masking.PermissionCodes;
import com.qq.ijay997.masking.UserContextHolder;
import com.qq.ijay997.masking.UserPrincipal;
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
 * mock 用户上下文过滤器。
 * <p>
 * 通过请求头模拟登录用户与权限码，写入 {@link UserContextHolder}，请求结束后必定清理：
 * <ul>
 *   <li>{@code X-User-Id}：用户标识，默认 1001；</li>
 *   <li>{@code X-Role}：ADMIN / OPERATOR / GUEST，默认 GUEST；</li>
 *   <li>{@code X-Permissions}：逗号分隔的权限码，存在时覆盖角色预置权限。</li>
 * </ul>
 * 默认 GUEST（空权限）配合"无权限即脱敏"，保证不携带请求头时不会泄露敏感数据。
 *
 * @author ijay997
 */
@Component
@Order(2)
public class MockUserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ROLE = "X-Role";
    private static final String HEADER_PERMISSIONS = "X-Permissions";

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
        return new UserPrincipal(userId, "mock-" + userId, role, permissions);
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
