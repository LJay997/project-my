package com.qq.ijay997.mp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.mp.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品控制器集成测试（MockMvc）。
 *
 * @author ijay997
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateAndGet() throws Exception {
        Product p = new Product();
        p.setName("控制器-" + System.nanoTime());
        p.setPrice(new BigDecimal("12.34"));
        p.setStock(8);

        String body = mockMvc.perform(post("/api/mp/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/mp/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(p.getName()))
                .andExpect(jsonPath("$.price").value(12.34));
    }

    @Test
    void testPageEndpoint() throws Exception {
        mockMvc.perform(get("/api/mp/products/page")
                        .param("current", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.current").value(1));
    }

    private static final String UNION_URL = "/api/mp/products/union";

    @Test
    @DisplayName("UNION 不带 X-Level：两个分支都不加条件，已分类(101)与未分类(111)都可见")
    void testUnionWithoutLevelHeader() throws Exception {
        Set<Long> ids = queryUnionIds(null);

        assertTrue(ids.contains(101L), "已分类商品(level=5)应出现在第一个分支");
        assertTrue(ids.contains(111L), "未分类商品应出现在第二个分支");
    }

    @Test
    @DisplayName("UNION + X-Level=4：两个分支各自追加 p.level <= 4")
    void testUnionFiltersEachBranch() throws Exception {
        Set<Long> ids = queryUnionIds("4");

        // 第一个分支：已分类商品里 level=5 的 101/103/114 被过滤
        assertFalse(ids.contains(101L), "level=5 的智能手机应被第一个分支过滤");
        assertFalse(ids.contains(103L), "level=5 的笔记本电脑应被第一个分支过滤");
        assertFalse(ids.contains(114L), "level=5 的蓝牙眼镜应被第一个分支过滤");
        assertTrue(ids.contains(102L), "level=4 的蓝牙耳机应保留");

        // 第二个分支：未分类商品(111, level=1)依然可见，证明 UNION 的第二个分支也被改写
        assertTrue(ids.contains(111L), "未分类商品(level=1)应保留，说明第二个分支同样生效");
    }

    @Test
    @DisplayName("UNION + X-Level=0：所有 level>=1 的商品都被过滤，结果为空")
    void testUnionWithZeroLevelReturnsEmpty() throws Exception {
        Set<Long> ids = queryUnionIds("0");

        assertTrue(ids.isEmpty(), "X-Level=0 时所有分支都只保留 level<=0 的商品，种子数据无此商品");
    }

    private Set<Long> queryUnionIds(String levelHeader) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = get(UNION_URL);
        if (levelHeader != null) {
            builder.header("X-Level", levelHeader);
        }

        String body = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode array = objectMapper.readTree(body);
        Set<Long> ids = new HashSet<>();
        array.forEach(node -> ids.add(node.get("id").asLong()));
        return ids;
    }
}