package com.qq.ijay997.masking;

import java.util.Collections;

/**
 * 当前请求用户上下文。
 * <p>
 * 由 {@code MockUserContextFilter} 在请求开始时写入、请求结束时清理。
 * 未设置上下文时按"匿名用户、无任何权限"处理，保证敏感字段默认脱敏（fail-closed）。
 *
 * @author ijay997
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private static final UserPrincipal ANONYMOUS =
            new UserPrincipal(null, "anonymous", "GUEST", Collections.emptySet());

    private UserContextHolder() {
    }

    public static void set(UserPrincipal principal) {
        if (principal == null) {
            HOLDER.remove();
        } else {
            HOLDER.set(principal);
        }
    }

    /**
     * 获取当前用户；无上下文时返回匿名用户（空权限）。
     */
    public static UserPrincipal get() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? ANONYMOUS : principal;
    }

    /**
     * 当前用户是否持有指定权限码；无上下文时始终返回 false。
     */
    public static boolean hasPermission(String permission) {
        return get().hasPermission(permission);
    }

    /**
     * 清理上下文，必须在请求结束时调用，避免线程池复用导致权限串号。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
