package com.qq.ijay997.masking;

import com.qq.ijay997.auth.UserContextHolder;

/**
 * 敏感字段统一判定入口。
 * <p>
 * JSON 序列化、EasyExcel 单元格写出、Bean 级预改写三条链路都调用本类，
 * 保证同一权限下三种输出使用完全一致的判定与规则：
 * <ul>
 *   <li>当前用户持有 {@link Sensitive#permission()} 对应权限码：返回原文；</li>
 *   <li>否则按 {@link Sensitive#type()} 交给 {@link MaskRules} 脱敏。</li>
 * </ul>
 *
 * @author ijay997
 */
public final class SensitiveValues {

    private SensitiveValues() {
    }

    /**
     * 按权限决定是否脱敏。
     *
     * @param sensitive 字段上的注解，可为 null
     * @param value     原始值，可为 null
     * @return 有权限时的原文，无权限时的脱敏值；入参为 null 时原样返回
     */
    public static String maskIfNeeded(Sensitive sensitive, String value) {
        if (sensitive == null || value == null) {
            return value;
        }
        if (UserContextHolder.hasPermission(sensitive.permission())) {
            return value;
        }
        return MaskRules.mask(sensitive.type(), value);
    }
}
