package com.qq.ijay997.mp;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 数据权限注解，标注在 MyBatis Mapper 查询方法上。
 * <p>
 * 由 {@link com.qq.ijay997.mp.interceptor.LevelInterceptor} 在 SQL 执行前拦截，
 * 使用 JSqlParser 解析 AST，为查询自动追加 {@code alias.column <= threshold} 的 WHERE 条件，
 * 实现基于 level 字段的行级数据权限控制。
 * <p>
 * <b>权限阈值来源：</b>当前登录用户的权限等级从
 * {@link com.qq.ijay997.auth.UserContextHolder#getLevel()} 获取（身份上下文 ThreadLocal），
 * 不在注解上配置（因为阈值因用户而异）。阈值为 {@code null} 时不追加任何条件。
 * <p>
 * <b>生效范围（已知局限）：</b>只改写主查询（PlainSelect）的 WHERE 子句，
 * 不递归进入 WHERE 中的子查询，UNION 等非 PlainSelect 顶层查询不做改写，
 * 也不覆盖 UPDATE/DELETE。
 * <p>
 * <b>别名机制：</b>{@link #value()} 是 {@link #tableAlias()} 的别名，
 * 依赖 Spring 的 {@code @AliasFor}。请通过 Spring 的注解工具
 * （如 {@code AnnotatedElementUtils.findMergedAnnotation()}）读取，
 * 而不是 JDK 原生的 {@code method.getAnnotation()}，否则 {@code @AliasFor} 不会生效。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 简写：只有一张表，别名为 "a"（默认值）
 * @Level
 * List<User> selectAll();
 *
 * // 简写：指定主表别名（value = tableAlias 的别名）
 * @Level("u")
 * List<User> selectByUser();
 *
 * // 显式：多表 JOIN 时指定主表别名和权限列名
 * @Level(tableAlias = "p", column = "level")
 * List<Product> selectProducts();
 * }</pre>
 *
 * <h3>两种用法对比</h3>
 * <p>
 * 数据权限条件有两种注入方式，拦截器自动改写和声明式 `${}` 占位符，可按需选择。</p>
 *
 * <h4>方式一：拦截器自动改写（当前实现）</h4>
 * <pre>{@code
 * // Mapper：只写注解，不关心 WHERE 怎么加
 * @Level("p")
 * @Select("SELECT * FROM product p WHERE p.price > 100")
 * List<Product> selectProducts();
 *
 * // 拦截器侧：读取注解 → 编译 LevelCondition → 改写 SQL AST
 * LevelConditionProvider provider = new LevelConditionProvider();
 * LevelCondition condition = provider.build(level, UserContextHolder.getLevel());
 * // → LevelCondition { alias="p", column="level", threshold=5 }
 *
 * String rewritten = provider.rewrite("SELECT * FROM product p WHERE p.price > 100", condition);
 * // → "SELECT * FROM product p WHERE p.price > 100 AND p.level <= 5"
 * }</pre>
 * <p>
 * <b>优点：</b>Mapper SQL 完全无感知，注解驱动，适合简单场景。<br>
 * <b>局限：</b>只改顶层 PlainSelect，子查询/UNION/UPDATE/DELETE 不覆盖。</p>
 *
 * <h4>方式二：声明式 {@code ${}} 占位符（推荐复杂场景）</h4>
 * <pre>{@code
 * // Mapper：直接在 SQL 里写 ${levelCondition} 占位符
 * @Select("SELECT * FROM product p " +
 *         "WHERE p.price > 100 " +
 *         "  AND ${levelCondition}")          // ← 直接嵌入 SQL 片段
 * List<Product> selectProducts(@Param("levelCondition") LevelCondition levelCondition);
 *
 * // Service 层：构造 LevelCondition 传入
 * LevelCondition condition = new LevelCondition("p", "level", user.getLevel());
 * // condition.toString() → "p.level <= 5"
 * productMapper.selectProducts(condition);
 * }</pre>
 * <p>
 * <b>优点：</b>SQL 全可见，子查询/UNION/UPDATE/DELETE 都能精确控制；
 * 甚至可以把条件拆到多个位置（如子查询里也加权限：{@code WHERE p2.level <= 5}）。<br>
 * <b>注意：</b>MyBatis 的 {@code ${}} 是纯字符串替换，存在 SQL 注入风险。
 * 本类的 {@link LevelCondition} 构造器已用正则 {@code [A-Za-z_][A-Za-z0-9_]*} 校验标识符，
 * 杜绝把恶意值拼成 SQL 片段，可以安全使用。</p>
 *
 * <h4>两种方式如何选择</h4>
 * <table>
 *   <tr><th>场景</th><th>推荐方式</th></tr>
 *   <tr><td>单表/多表 JOIN，只改顶层 WHERE</td><td>拦截器自动改写</td></tr>
 *   <tr><td>子查询也要加权限条件</td><td>{@code ${}} 占位符</td></tr>
 *   <tr><td>UPDATE / DELETE 也需要权限</td><td>{@code ${}} 占位符</td></tr>
 *   <tr><td>多个权限字段（如 level + tenant_id）</td><td>{@code ${}} 占位符</td></tr>
 * </table>
 *
 * <h3>LevelCondition 的安全设计</h3>
 * <p>
 * 因为要嵌入 {@code ${}}，{@link LevelCondition} 构造器强制对 alias / column 做白名单校验：</p>
 * <pre>{@code
 * new LevelCondition("p", "level", 5)                       // ✅ 合法："p.level <= 5"
 * new LevelCondition("p; DROP TABLE mp_product", "level", 5) // ❌ 抛 IllegalArgumentException
 * new LevelCondition("p OR 1=1", "level", 5)                // ❌ 抛 IllegalArgumentException
 * }</pre>
 * <p>
 * 阈值 {@code threshold} 是 {@code int} 类型，天然不可能注入。三者组合后 {@link LevelCondition#toString()}
 * 只会产出形如 {@code alias.column <= int} 的安全 SQL 片段。</p>
 *
 * <h3>拦截器改写效果</h3>
 * <pre>{@code
 * // 原 SQL:     SELECT * FROM product p WHERE p.price > 100
 * // 注解:       @Level("p")
 * // 阈值:       UserContextHolder.getLevel() = 5
 * // 改写后:     SELECT * FROM product p WHERE p.price > 100 AND p.level <= 5
 *
 * // 原 SQL:     SELECT * FROM product p WHERE p.deleted = 0  (无 WHERE 条件)
 * // 注解:       @Level("p")
 * // 改写后:     SELECT * FROM product p WHERE p.deleted = 0 AND p.level <= 5
 *              （注：这里拦截器不会改原有 WHERE，而是追加 AND）
 *
 * // 原 SQL:     SELECT * FROM product p WHERE p.status = 1 OR p.status = 2
 * // 注解:       @Level("p")
 * // 改写后:     SELECT * FROM product p WHERE (p.status = 1 OR p.status = 2) AND p.level <= 5
 *              （OR 表达式自动加括号，避免优先级错误）
 * }</pre>
 *
 * @author ijay997
 * @see LevelCondition
 * @see LevelConditionProvider
 * @see com.qq.ijay997.mp.interceptor.LevelInterceptor
 * @see com.qq.ijay997.auth.UserContextHolder
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface Level {

    /**
     * 主表别名（快捷写法），等价于 {@link #tableAlias()}。
     * <p>
     * 使用 {@code @AliasFor} 关联到 {@code tableAlias}，
     * 依赖 Spring 的注解合并机制生效。
     * <p>
     * 与 {@code tableAlias} 必须声明相同的默认值 {@code "a"}，否则 Spring 在注解合成时会抛出
     * {@code AnnotationConfigurationException}。
     * <p>
     * 推荐简写：{@code @Level("u")} 等价于 {@code @Level(tableAlias = "u")}。
     */
    @AliasFor(attribute = "tableAlias")
    String value() default "a";

    /**
     * 主表别名，用于在 SQL 中引用权限字段。
     * <p>
     * 拦截器会生成 {@code {tableAlias}.{column} <= threshold} 的条件，
     * 所以别名必须与你的 SQL 中实际的表别名一致。
     * <p>
     * 默认值 {@code "a"}：单表查询通常没有显式别名，
     * 拦截器在这种场景下生成的条件是 {@code a.level <= ?}，
     * 但你的 SQL 里表别名不是 "a" 的话会产生无效 SQL。
     * <b>建议：多表 JOIN 或自定义别名时，务必显式指定此值。</b>
     */
    @AliasFor("value")
    String tableAlias() default "a";

    /**
     * 存储权限等级的数据库列名。
     * <p>
     * 拦截器会生成 {@code {tableAlias}.{column} <= threshold} 的条件。
     * <p>
     * 默认 {@code "level"}，可根据你的表结构覆盖为其他名称（如 {@code data_level}）。
     */
    String column() default "level";
}