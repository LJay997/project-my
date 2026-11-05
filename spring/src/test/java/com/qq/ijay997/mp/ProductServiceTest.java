package com.qq.ijay997.mp;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qq.ijay997.mp.entity.Product;
import com.qq.ijay997.mp.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MyBatis-Plus 商品服务集成测试：验证 CRUD、自动填充、乐观锁、逻辑删除、分页与条件构造器。
 *
 * @author ijay997
 */
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Product newProduct(String name, String price, int stock) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        return p;
    }

    @Test
    void testInsertWithAutoFill() {
        Product p = newProduct("MP-自动填充-" + System.nanoTime(), "9.90", 100);
        productService.save(p);

        assertNotNull(p.getId(), "雪花主键应自动生成");
        assertEquals(0, p.getVersion());
        assertEquals(0, p.getDeleted());
        assertNotNull(p.getCreateTime(), "插入时应自动填充 createTime");
        assertNotNull(p.getUpdateTime(), "插入时应自动填充 updateTime");
    }

    @Test
    void testSelectByIdAndUpdate() {
        Product p = newProduct("MP-更新-" + System.nanoTime(), "19.90", 10);
        productService.save(p);

        Product loaded = productService.getById(p.getId());
        assertNotNull(loaded);
        assertEquals("19.90", loaded.getPrice().toPlainString());

        loaded.setStock(20);
        boolean updated = productService.updateById(loaded);
        assertTrue(updated);

        Product after = productService.getById(p.getId());
        assertEquals(20, after.getStock());
        assertEquals(1, after.getVersion(), "更新后乐观锁版本号应 +1");
    }

    @Test
    void testOptimisticLockStaleUpdateFails() {
        Product p = newProduct("MP-并发-" + System.nanoTime(), "29.90", 5);
        productService.save(p);

        // 首次更新成功，版本 0 -> 1
        Product first = productService.getById(p.getId());
        first.setStock(6);
        assertTrue(productService.updateById(first));

        // 使用过期版本号再次更新应失败（version 仍为 0，但库中已是 1）
        Product stale = new Product();
        stale.setId(p.getId());
        stale.setVersion(0);
        stale.setStock(999);
        assertFalse(productService.updateById(stale), "过期版本号应导致更新失败");

        Product current = productService.getById(p.getId());
        assertEquals(6, current.getStock(), "过期更新不应影响库存");
        assertEquals(1, current.getVersion());
    }

    @Test
    void testLogicDelete() {
        Product p = newProduct("MP-删除-" + System.nanoTime(), "39.90", 3);
        productService.save(p);
        Long id = p.getId();

        assertTrue(productService.removeById(id));

        // 逻辑删除后，MyBatis-Plus 查询自动过滤
        assertNull(productService.getById(id));

        // 但物理记录仍存在，仅 deleted 被置为 1
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM mp_product WHERE id = ?", Integer.class, id);
        assertEquals(1, deleted);
    }

    @Test
    void testConditionQuery() {
        String suffix = String.valueOf(System.nanoTime());
        productService.save(newProduct("苹果-" + suffix, "9.90", 10));
        productService.save(newProduct("香蕉-" + suffix, "19.90", 10));
        productService.save(newProduct("苹果派-" + suffix, "29.90", 10));

        // 名称包含“苹果”且价格在 5~15 之间
        assertEquals(1, productService.listByCondition("苹果", new BigDecimal("5"), new BigDecimal("15")).size());
        // 名称包含“苹果”（不限价格）
        assertEquals(2, productService.listByCondition("苹果", null, null).size());
    }

    @Test
    void testPagination() {
        String suffix = String.valueOf(System.nanoTime());
        for (int i = 0; i < 7; i++) {
            productService.save(newProduct("分页-" + suffix + "-" + i, "50.00", 1));
        }

        IPage<Product> page = productService.pageByPrice(new BigDecimal("49"), 1, 3);
        assertTrue(page.getTotal() >= 7);
        assertEquals(3, page.getRecords().size(), "每页大小应为 3");
        assertEquals(1, page.getCurrent());
    }
}