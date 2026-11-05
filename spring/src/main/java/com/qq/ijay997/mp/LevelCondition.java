package com.qq.ijay997.mp;

import java.util.regex.Pattern;

/**
 * 数据权限条件值对象：{@code alias.column <= threshold}。
 * <p>
 * 由 {@link LevelConditionProvider} 生成，供 {@code LevelInterceptor} 改写 SQL 使用。
 * 构造时对表别名与列名做标识符白名单校验，杜绝把注解值拼成非法/恶意 SQL 片段。
 *
 * @author ijay997
 */
public final class LevelCondition {

    /** SQL 标识符白名单：字母或下划线开头，仅含字母、数字、下划线。 */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final String alias;

    private final String column;

    private final int threshold;

    public LevelCondition(String alias, String column, int threshold) {
        this.alias = alias == null ? "" : alias.trim();
        if (!this.alias.isEmpty() && !IDENTIFIER.matcher(this.alias).matches()) {
            throw new IllegalArgumentException("非法的数据权限表别名: " + alias);
        }
        if (column == null || !IDENTIFIER.matcher(column.trim()).matches()) {
            throw new IllegalArgumentException("非法的数据权限列名: " + column);
        }
        this.column = column.trim();
        this.threshold = threshold;
    }

    public String getAlias() {
        return alias;
    }

    public String getColumn() {
        return column;
    }

    public int getThreshold() {
        return threshold;
    }

    /**
     * 是否带表别名；为空表示使用未限定列名（如 {@code level <= 5}）。
     */
    public boolean hasAlias() {
        return !alias.isEmpty();
    }

    @Override
    public String toString() {
        return (hasAlias() ? alias + "." : "") + column + " <= " + threshold;
    }
}
