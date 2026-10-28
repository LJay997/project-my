package com.qq.ijay997.masking;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 权限码常量与 mock 角色的预置权限映射。
 *
 * @author ijay997
 */
public final class PermissionCodes {

    /** 查看用户名明文 */
    public static final String USER_NAME_READ = "user:name:read";

    /** 查看邮箱明文 */
    public static final String USER_EMAIL_READ = "user:email:read";

    /** 查看手机号明文 */
    public static final String USER_PHONE_READ = "user:phone:read";

    /** 全部权限 */
    public static final Set<String> ALL = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(USER_NAME_READ, USER_EMAIL_READ, USER_PHONE_READ)));

    /** 无任何权限 */
    public static final Set<String> NONE = Collections.emptySet();

    private PermissionCodes() {
    }

    /**
     * mock 角色 -&gt; 权限码集合。未知角色按最小权限（空集合）处理，遵循 fail-closed。
     *
     * @param role 角色名（不区分大小写）
     * @return 不可变权限集合
     */
    public static Set<String> forRole(String role) {
        if (role == null) {
            return NONE;
        }
        switch (role.trim().toUpperCase()) {
            case "ADMIN":
                return ALL;
            case "OPERATOR":
                return Collections.singleton(USER_NAME_READ);
            case "GUEST":
            default:
                return NONE;
        }
    }
}
