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
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.sql.SQLException;

/**
 * 数据权限条件生成器：把 {@link Level} 注解 + 阈值编译为 {@link LevelCondition}，
 * 再改写 SQL AST 追加条件。
 * <p>
 * 职责边界：只做"条件建模 + SQL 改写"，不读取注解、不读取 ThreadLocal、不碰 MyBatis 生命周期；
 * 这些由 {@code LevelInterceptor} 负责。
 * <p>
 * <b>已知局限：</b>只处理顶层 {@link PlainSelect}，不递归 WHERE 内的子查询，也不处理 UNION 各分支。
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
            // TODO 待使用 select.getSelectBody().accept() 增加处理 UNION 的场景
            processSelectBody(select.getSelectBody(), condition);
            return select.toString();
        } catch (JSQLParserException e) {
            throw new SQLException("SQL 解析失败: " + sql, e);
        }
    }

    /**
     * 处理顶层查询 —— 只给主查询追加权限条件，不递归子查询和 UNION 分支。
     */
    private void processSelectBody(Select body, LevelCondition condition) {
        if (!(body instanceof PlainSelect)) {
            return;
        }
        processPlainSelect((PlainSelect) body, condition);
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
