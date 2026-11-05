package com.qq.ijay997.mp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link LevelConditionProvider} 回归测试：锁定重构前后改写结果逐字一致，
 * 并验证标识符白名单与空阈值处理。
 *
 * @author ijay997
 */
class LevelConditionProviderTest {

    private final LevelConditionProvider provider = new LevelConditionProvider();

    @Test
    @DisplayName("原有 WHERE：追加 AND 条件，结果逐字一致")
    void shouldAppendConditionWhenWhereExists() throws Exception {
        LevelCondition condition = new LevelCondition("p", "level", 5);

        assertEquals(
                "SELECT * FROM mp_product p WHERE p.price > 100 AND p.level <= 5",
                provider.rewriteSelectSql("SELECT * FROM mp_product p WHERE p.price > 100", condition));
    }

    @Test
    @DisplayName("无 WHERE：直接作为 WHERE，结果逐字一致")
    void shouldSetConditionWhenWhereAbsent() throws Exception {
        LevelCondition condition = new LevelCondition("p", "level", 5);

        assertEquals(
                "SELECT * FROM mp_product p WHERE p.level <= 5",
                provider.rewriteSelectSql("SELECT * FROM mp_product p", condition));
    }

    @Test
    @DisplayName("OR 条件：整体加括号后追加，避免优先级错误")
    void shouldWrapOrWhereInParenthesis() throws Exception {
        LevelCondition condition = new LevelCondition("p", "level", 5);

        assertEquals(
                "SELECT * FROM mp_product p WHERE (p.status = 1 OR p.status = 2) AND p.level <= 5",
                provider.rewriteSelectSql("SELECT * FROM mp_product p WHERE p.status = 1 OR p.status = 2", condition));
    }

    @Test
    @DisplayName("非 SELECT 语句：原样返回")
    void shouldKeepNonSelectSqlUntouched() throws Exception {
        LevelCondition condition = new LevelCondition("p", "level", 5);
        String update = "UPDATE mp_product SET stock = 1 WHERE id = 101";

        assertEquals(update, provider.rewriteSelectSql(update, condition));
    }

    @Test
    @DisplayName("阈值为 null：不生成条件，调用方保持休眠")
    void shouldReturnNullWhenThresholdAbsent() {
        Method method = method("annotatedWithAlias");
        Level level = AnnotatedElementUtils.findMergedAnnotation(method, Level.class);

        assertNull(provider.build(level, null));
    }

    @Test
    @DisplayName("@Level(\"p\") 通过 @AliasFor 解析为表别名 p")
    void shouldResolveAliasFromValue() {
        Method method = method("annotatedWithAlias");
        Level level = AnnotatedElementUtils.findMergedAnnotation(method, Level.class);

        assertEquals("p", provider.build(level, 5).getAlias());
    }

    @Test
    @DisplayName("非法标识符：拒绝生成条件")
    void shouldRejectIllegalIdentifier() {
        assertThrows(IllegalArgumentException.class,
                () -> new LevelCondition("p", "level; DROP TABLE mp_product", 5));
        assertThrows(IllegalArgumentException.class,
                () -> new LevelCondition("p OR 1=1", "level", 5));
    }

    @Test
    @DisplayName("空别名：生成未限定列名")
    void shouldAllowEmptyAlias() {
        assertEquals("level <= 5", new LevelCondition("", "level", 5).toString());
    }

    private Method method(String name) {
        try {
            return LevelConditionProviderTest.class.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Level("p")
    private void annotatedWithAlias() {
        // 仅用于读取注解
    }
}
