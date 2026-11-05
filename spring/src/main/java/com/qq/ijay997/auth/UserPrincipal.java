package com.qq.ijay997.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前请求用户（mock）。不可变对象，可在多个组件间安全共享。
 * <p>
 * {@code level} 是数据权限等级阈值，原 {@code com.qq.ijay997.mp.UserLevelHolder} 的 ThreadLocal
 * 已合并到这里，避免两个独立 ThreadLocal 各自清理导致状态不一致。{@code level} 为 {@code null}
 * 表示"未设置阈值"，数据权限拦截器此时不追加任何条件。
 *
 * @author ijay997
 */
public final class UserPrincipal {

    private final String userId;

    private final String username;

    private final String role;

    private final Set<String> permissions;

    private final Integer level;

    public UserPrincipal(String userId, String username, String role, Set<String> permissions) {
        this(userId, username, role, permissions, null);
    }

    public UserPrincipal(String userId, String username, String role, Set<String> permissions, Integer level) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.permissions = permissions == null
                ? PermissionCodes.NONE
                : Collections.unmodifiableSet(new LinkedHashSet<>(permissions));
        this.level = level;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * 数据权限等级阈值；{@code null} 表示未设置，拦截器不追加条件。
     * <p>
     * 加 {@link JsonIgnore} 是为了保持 {@code /api/demo/masking/me} 的既有响应结构不变。
     */
    @JsonIgnore
    public Integer getLevel() {
        return level;
    }

    /**
     * 是否持有指定权限码。
     */
    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }

    /**
     * 返回带指定等级阈值的副本，其余身份信息不变。
     */
    public UserPrincipal withLevel(Integer level) {
        return new UserPrincipal(userId, username, role, permissions, level);
    }

    @Override
    public String toString() {
        return "UserPrincipal{"
                + "userId='" + userId + '\''
                + ", username='" + username + '\''
                + ", role='" + role + '\''
                + ", permissions=" + permissions
                + ", level=" + level
                + '}';
    }
}
