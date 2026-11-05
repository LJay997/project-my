package com.qq.ijay997.mp;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.TableStatement;
import net.sf.jsqlparser.statement.select.Values;
import net.sf.jsqlparser.statement.select.WithItem;

import java.sql.SQLException;

/**
 * 数据权限条件生成器：把 {@link Level} 注解 + 阈值编译为 {@link LevelCondition}，
 * 再改写 SQL AST 追加条件。
 * <p>
 * 职责边界：只做"条件建模 + SQL 改写"，不读取注解、不读取 ThreadLocal、不碰 MyBatis 生命周期；
 * 这些由 {@code LevelInterceptor} 负责。
 * <p>
 * <b>处理范围：</b>借助 {@link SelectVisitor} 处理顶层 {@link PlainSelect} 以及 UNION 的每个分支；
 * 不递归 WHERE 内部的子查询，也不处理 CTE（{@link WithItem}）自身的查询体。
 *
 * @author ijay997
 */
public final class LevelConditionProvider {

    /**
     * 由注解与阈值构造条件对象。阈值为 {@code null} 时返回 {@code null}，表示不追加条件。
     */
    public LevelCondition build(Level level, Integer threshold) {
        if (level == null || threshold == null) {
            return null;
        }
        return new LevelCondition(resolveAlias(level), level.column(), threshold);
    }

    /**
     * 构造 {@code alias.column <= threshold} 的 AST 节点。
     */
    public Expression toExpression(LevelCondition condition) {
        Column column = condition.hasAlias()
                ? new Column(new Table(condition.getAlias()), condition.getColumn())
                : new Column(condition.getColumn());

        MinorThanEquals expression = new MinorThanEquals();
        expression.setLeftExpression(column);
        expression.setRightExpression(new LongValue(condition.getThreshold()));
        return expression;
    }

    /**
     * 解析 SQL 并追加权限条件；非 SELECT 语句原样返回。
     */
    public String rewriteSelectSql(String sql, LevelCondition condition) throws SQLException {
        if (condition == null) {
            return sql;
        }
        try {
            // 重点 API
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                return sql;
            }
            Select select = (Select) stmt;
            select.getSelectBody().accept(new LevelSelectVisitor(condition));
            return select.toString();
        } catch (JSQLParserException e) {
            throw new SQLException("SQL 解析失败: " + sql, e);
        }
    }

    /**
     * 给 PlainSelect 追加条件，正确处理 WHERE 为 null / OR 表达式 / 普通条件。
     */
    private void processPlainSelect(PlainSelect select, LevelCondition condition) {
        // CCJSqlParserUtil.parseCondExpression("p.level <= 3"); 反推 Expression
        Expression levelCondition = toExpression(condition);
        Expression where = select.getWhere();

        if (where == null) {
            select.setWhere(levelCondition);
        } else if (where instanceof OrExpression) {
            // OR 表达式必须加括号,否则会变成 a=1 OR (b=2 AND level<=5),语义错误
            select.setWhere(new AndExpression(new Parenthesis(where), levelCondition));
        } else {
            select.setWhere(new AndExpression(where, levelCondition));
        }
    }

    /**
     * 遍历查询体，把权限条件追加到每个 {@link PlainSelect}。
     * <p>
     * 三种结构会递归下去：</p>
     * <ul>
     *     <li>普通单查询 —— {@link PlainSelect}，直接改写 WHERE</li>
     *     <li>UNION / INTERSECT / MINUS —— {@link SetOperationList}，逐个分支递归</li>
     *     <li>括号子查询 —— {@link ParenthesedSelect}，递归其内部查询</li>
     * </ul>
     * <p>
     * 三类显式跳过：{@link WithItem}（CTE 定义体，条件应加在引用 CTE 的外层查询上）、
     * {@link Values} / {@link TableStatement}（没有 WHERE 子句）。
     * {@link LateralSubSelect} 虽实现了递归，但位于 FROM 子句，当前遍历链路不会命中。</p>
     */
    private class LevelSelectVisitor implements SelectVisitor {

        private final LevelCondition condition;

        LevelSelectVisitor(LevelCondition condition) {
            this.condition = condition;
        }

        /**
         * 普通单表/多表查询 —— 改写 WHERE，是唯一真正"加条件"的分支。
         * <p>
         * 触发场景：</p>
         * <pre>{@code
         * SELECT * FROM mp_product p                          -- WHERE 为 null，直接设为条件
         * SELECT * FROM mp_product p WHERE p.price > 100      -- 追加 AND
         * SELECT * FROM mp_product p WHERE a = 1 OR b = 2     -- 整体加括号后追加 AND
         * }</pre>
         * <p>
         * 也包含 JOIN、DISTINCT、GROUP BY、HAVING、ORDER BY 等，它们都是 {@link PlainSelect} 的组成部分，
         * 条件统一加在 WHERE 上，不影响其它子句。</p>
         */
        @Override
        public void visit(PlainSelect plainSelect) {
            processPlainSelect(plainSelect, condition);
        }

        /**
         * 集合运算查询（UNION / UNION ALL / INTERSECT / EXCEPT / MINUS）—— 逐个分支递归改写。
         * <p>
         * 触发场景：</p>
         * <pre>{@code
         * SELECT p.* FROM mp_product p WHERE p.deleted = 0 AND p.category_id IS NOT NULL
         * UNION ALL
         * SELECT p.* FROM mp_product p WHERE p.deleted = 0 AND p.category_id IS NULL
         * }</pre>
         * <p>
         * 关键点：{@code SetOperationList.accept()} 自身<b>不会</b>遍历分支，只回调本方法一次，
         * 因此必须在这里手动 {@code forEach} 递归，否则 UNION 的每个 SELECT 都不会被改写。
         * 递归后每个分支各自得到一份完整的 {@code alias.column <= threshold}，互不影响。</p>
         */
        @Override
        public void visit(SetOperationList setOperationList) {
            for (Select select : setOperationList.getSelects()) {
                select.accept(this);
            }
        }

        /**
         * 括号包裹的查询 —— 剥掉括号后递归处理内部查询。
         * <p>
         * 触发场景：</p>
         * <pre>{@code
         * (SELECT * FROM mp_product p WHERE p.price > 100) UNION (SELECT * FROM mp_product p)
         * }</pre>
         * <p>
         * UNION 的每个分支常被括号包裹，解析后即为 {@link ParenthesedSelect}。
         * 不剥括号就无法命中内部的 {@link PlainSelect}，条件会漏加。</p>
         */
        @Override
        public void visit(ParenthesedSelect parenthesedSelect) {
            parenthesedSelect.getSelect().accept(this);
        }

        /**
         * CTE（{@code WITH ... AS (...)}）的定义体 —— 有意跳过，不加条件。
         * <p>
         * 触发场景：</p>
         * <pre>{@code
         * WITH t AS (SELECT * FROM mp_product x WHERE x.stock > 0) SELECT * FROM mp_product p
         * }</pre>
         * <p>
         * 跳过原因：CTE 只是临时命名结果集，真正的数据出口是引用它的外层查询。
         * 条件应加在外层 {@code SELECT * FROM mp_product p} 的 WHERE 上（由 {@link #visit(PlainSelect)} 负责）。
         * 若在 CTE 内部也加条件，会因别名 {@code x} 与注解别名 {@code p} 不一致而生成非法 SQL。</p>
         */
        @Override
        public void visit(WithItem withItem) {
        }

        /**
         * {@code VALUES} 行构造器 —— 跳过，没有 WHERE 可加。
         * <p>
         * 触发场景（VALUES 直接作为查询体）：</p>
         * <pre>{@code
         * VALUES (1, 'a'), (2, 'b')
         * }</pre>
         * <p>
         * 这类语句没有表、没有 WHERE，追加 {@code p.level <= 5} 会直接语法错误，故原样返回。</p>
         * <p>
         * <b>注意区分：</b>{@code SELECT * FROM (VALUES (1,'a'),(2,'b')) AS t(id, name)} 的查询体是
         * {@link PlainSelect}（VALUES 只是它的 FROM 项），会走 {@link #visit(PlainSelect)} 给外层加条件；
         * 但该场景没有别名 {@code p}，生成的 SQL 会因列不存在而报错。VALUES 派生表场景请改用
         * {@code ${}} 占位符方式。</p>
         */
        @Override
        public void visit(Values values) {
        }

        /**
         * {@code TABLE} 简写查询 —— 跳过，没有 WHERE 可加。
         * <p>
         * 触发场景：</p>
         * <pre>{@code
         * TABLE mp_product      -- 等价于 SELECT * FROM mp_product
         * }</pre>
         * <p>
         * 这是 PostgreSQL/SQL 标准支持的简写，{@link TableStatement} 不持有 WHERE 节点，
         * 强行改写需要先转换成 {@link PlainSelect}，成本高于收益，故跳过。</p>
         */
        @Override
        public void visit(TableStatement tableStatement) {
        }

        /**
         * LATERAL 派生表（{@code LATERAL (...)}）—— 实现仅为满足接口契约，当前遍历链路不会走到这里。
         * <p>
         * 触发场景（语法上存在，但本访问器不会命中）：</p>
         * <pre>{@code
         * SELECT p.*, t.* FROM mp_product p
         * LEFT JOIN LATERAL (SELECT * FROM mp_category c WHERE c.id = p.category_id) t ON TRUE
         * }</pre>
         * <p>
         * <b>为什么不会命中：</b>{@link LateralSubSelect} 继承自 {@link ParenthesedSelect}，
         * 但它出现在 FROM 子句里，属于 {@code FromItem} 而非查询体。本访问器由
         * {@code select.getSelectBody().accept(...)} 启动，只沿查询体链（PlainSelect / SetOperationList /
         * ParenthesedSelect）递归，FROM 项不在遍历范围内。</p>
         * <p>
         * 因此 LATERAL 内部查询<b>不会被追加权限条件</b>（实测确认）。若该子查询也需要权限过滤，
         * 请用 {@code ${}} 占位符在 SQL 里显式声明条件，不要依赖本拦截器。</p>
         */
        @Override
        public void visit(LateralSubSelect lateralSubSelect) {
            lateralSubSelect.getSelect().accept(this);
        }
    }

    /**
     * 解析表别名。
     * <p>{@link Level#value()} 通过 {@link org.springframework.core.annotation.AliasFor} 声明为
     * {@link Level#tableAlias()} 的别名。这里做兜底：当 tableAlias 仍是默认值 "a" 而 value 被显式指定时，以 value 为准。</p>
     */
    private String resolveAlias(Level level) {
        String tableAlias = level.tableAlias();
        String value = level.value();
        boolean tableAliasIsDefault = tableAlias == null || tableAlias.isEmpty() || "a".equals(tableAlias);
        if (tableAliasIsDefault && value != null && !value.isEmpty() && !"a".equals(value)) {
            return value;
        }
        return tableAlias;
    }
}