package com.qq.ijay997.auth;

import java.util.Collections;

/**
 * 当前请求用户上下文（唯一身份 ThreadLocal）。
 * <p>
 * 由 {@code MockUserContextFilter} 在请求开始时写入、请求结束时清理。
 * 未设置上下文时按"匿名用户、无任何权限"处理，保证敏感字段默认脱敏（fail-closed）。
 * <p>
 * 数据权限阈值 {@code level} 已并入 {@link UserPrincipal}，因此整个项目只有一个身份 ThreadLocal，
 * 不存在"权限上下文已清理、等级上下文残留"的双 ThreadLocal 不一致问题。
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
     * 获取当前请求的数据权限等级阈值；未设置上下文或未设置阈值时返回 {@code null}，
     * 调用方（{@code LevelInterceptor}）据此保持"休眠"，不追加任何条件。
     */
    public static Integer getLevel() {
        UserPrincipal principal = HOLDER.get();
        return principal == null ? null : principal.getLevel();
    }

    /**
     * 为当前请求设置数据权限等级阈值，保留已有身份与权限信息。
     * <p>
     * 无上下文时会创建一个匿名主体承载该阈值；仅用于演示/测试，生产环境应由认证流程
     * 直接 {@link #set(UserPrincipal)} 完整的 {@link UserPrincipal}。
     */
    public static void setLevel(Integer level) {
        UserPrincipal current = HOLDER.get();
        if (current == null) {
            HOLDER.set(new UserPrincipal(null, "anonymous", "GUEST", Collections.emptySet(), level));
        } else {
            HOLDER.set(current.withLevel(level));
        }
    }

    /**
     * 清理上下文，必须在请求结束时调用，避免线程池复用导致权限串号。
     * <p>
     * 清理后 {@link #getLevel()} 必然回到 {@code null}，即拦截器回到"不追加条件"状态。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * {@link #clear()} 的语义别名，便于从原 {@code UserLevelHolder.remove()} 迁移。
     */
    public static void remove() {
        clear();
    }
}
