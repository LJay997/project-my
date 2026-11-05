package com.qq.ijay997.mp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code X-Level} 请求头集成测试。
 * <p>
 * 走 MockMvc + {@link com.qq.ijay997.auth.MockUserContextFilter} 全链路，验证请求头能真正
 * 驱动 {@link com.qq.ijay997.mp.interceptor.LevelInterceptor}。选择新增测试类而不是并入
 * {@link SubqueryQueryTest}：原测试类用编程式 {@code setLevel} 验证拦截器逻辑，本类验证
 * "HTTP → 身份上下文 → 拦截器"的适配层，职责不同，互不影响。
 *
 * @author ijay997
 */
@SpringBootTest
@AutoConfigureMockMvc
class LevelHeaderIntegrationTest {

    private static final String ABOVE_AVG_URL = "/api/mp/categories/products/above-avg";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("不带 X-Level：拦截器休眠，level=5 的蓝牙眼镜(114)可见")
    void shouldNotFilterWithoutLevelHeader() throws Exception {
        Set<Long> ids = queryProductIds(null);

        assertTrue(ids.contains(114L), "未设置阈值时不应追加任何等级条件");
    }

    @Test
    @DisplayName("X-Level=4：外层条件生效，level=5 的蓝牙眼镜(114)被过滤")
    void shouldFilterByValidLevelHeader() throws Exception {
        Set<Long> ids = queryProductIds("4");

        assertFalse(ids.contains(114L), "X-Level=4 应过滤 level=5 的商品");
        assertTrue(ids.contains(108L), "X-Level=4 不应影响 level<=4 的商品");
    }

    @Test
    @DisplayName("X-Level 非法/空白/负数：等同于未设置，返回 200 且不过滤")
    void shouldTreatInvalidLevelAsUnset() throws Exception {
        for (String invalid : new String[]{"abc", "", "   ", "-1", "4.5", "999999999999"}) {
            Set<Long> ids = queryProductIds(invalid);
            assertTrue(ids.contains(114L), "非法 X-Level 不应影响查询: [" + invalid + "]");
        }
    }

    private Set<Long> queryProductIds(String levelHeader) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = get(ABOVE_AVG_URL);
        if (levelHeader != null) {
            builder.header("X-Level", levelHeader);
        }

        String body = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode array = objectMapper.readTree(body);
        Set<Long> ids = new HashSet<>();
        array.forEach(node -> ids.add(node.get("productId").asLong()));
        return ids;
    }
}
