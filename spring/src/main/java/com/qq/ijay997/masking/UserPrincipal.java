package com.qq.ijay997.masking;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 当前请求用户（mock）。不可变对象，可在多个组件间安全共享。
 *
 * @author ijay997
 */
public final class UserPrincipal {

    private final String userId;

    private final String username;

    private final String role;

    private final Set<String> permissions;

    public UserPrincipal(String userId, String username, String role, Set<String> permissions) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.permissions = permissions == null
                ? PermissionCodes.NONE
                : Collections.unmodifiableSet(new LinkedHashSet<>(permissions));
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
     * 是否持有指定权限码。
     */
    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "UserPrincipal{"
                + "userId='" + userId + '\''
                + ", username='" + username + '\''
                + ", role='" + role + '\''
                + ", permissions=" + permissions
                + '}';
    }
}
