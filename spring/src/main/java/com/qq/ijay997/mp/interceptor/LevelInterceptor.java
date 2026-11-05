package com.qq.ijay997.mp.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.qq.ijay997.auth.UserContextHolder;
import com.qq.ijay997.mp.Level;
import com.qq.ijay997.mp.LevelCondition;
import com.qq.ijay997.mp.LevelConditionProvider;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 数据权限拦截器。
 * <p>
 * 只负责三件事：解析 Mapper 方法上的 {@link Level} 注解、从 {@link UserContextHolder} 读取当前
 * 请求的等级阈值、委托 {@link LevelConditionProvider} 改写 SQL。条件的建模与 AST 改写细节
 * 全部在 Provider 中，避免拦截器职责过重。
 * <p>
 * <b>阈值来源:</b>{@link UserContextHolder#getLevel()}。为 {@code null}（未设置上下文或已清理）时
 * 直接返回，不追加任何条件，保持"休眠"语义。
 * <p>
 * <b>已知局限:</b>只改写顶层 PlainSelect，不递归子查询、不处理 UNION 分支、不覆盖 UPDATE/DELETE。
 */
public class LevelInterceptor implements InnerInterceptor {

    private static final ConcurrentMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private static final LevelConditionProvider CONDITION_PROVIDER = new LevelConditionProvider();

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Method method = resolveMapperMethod(ms.getId());
        if (method == null) {
            return;
        }

        // 使用 Spring 注解工具读取,@AliasFor 在这里才会生效 method.getAnnotation(Level.class)
        Level level = AnnotatedElementUtils.findMergedAnnotation(method, Level.class);
        if (level == null) {
            return;
        }
        LevelCondition condition = CONDITION_PROVIDER.build(level, UserContextHolder.getLevel());
        if (condition == null) {
            return;
        }

        String originalSql = boundSql.getSql();
        String newSql = CONDITION_PROVIDER.rewriteSelectSql(originalSql, condition);
        // BoundSql 无 setSql(String)，使用 MyBatis-Plus 提供的包装器改写（与分页拦截器同一机制）
        PluginUtils.mpBoundSql(boundSql).sql(newSql);
    }

    private Method resolveMapperMethod(String msId) {
        return METHOD_CACHE.computeIfAbsent(msId, id -> {
            try {
                int lastDot = id.lastIndexOf('.');
                String className = id.substring(0, lastDot);
                String methodName = id.substring(lastDot + 1);
                Class<?> mapperClass = Class.forName(className);
                for (Method m : mapperClass.getMethods()) {
                    if (m.getName().equals(methodName)) {
                        return m;
                    }
                }
                return null;
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
    }
}
