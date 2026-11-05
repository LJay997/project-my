package com.qq.ijay997.mp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qq.ijay997.mp.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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
}