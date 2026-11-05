package com.qq.ijay997.mp;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qq.ijay997.mp.entity.Category;
import com.qq.ijay997.mp.entity.Product;
import com.qq.ijay997.mp.service.CategoryService;
import com.qq.ijay997.mp.service.ProductService;
import com.qq.ijay997.mp.vo.ProductCategoryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品分类服务集成测试：验证分类 CRUD、level 字段写入/查询，以及商品-分类关联查询（列表/详情/分页）。
 *
 * @author ijay997
 */
@SpringBootTest
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    private Category newCategory(String name, int level) {
        Category c = new Category();
        c.setName(name);
        c.setLevel(level);
        return c;
    }

    private Product newProduct(String name, String price, int stock, Integer level, Long categoryId) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setLevel(level);
        p.setCategoryId(categoryId);
        return p;
    }

    @Test
    void testInsertCategoryWithLevel() {
        Category c = newCategory("手机-" + System.nanoTime(), 1);
        categoryService.save(c);
        assertNotNull(c.getId(), "分类主键应自动生成");
        assertEquals(1, c.getLevel());

        Category loaded = categoryService.getById(c.getId());
        assertEquals(1, loaded.getLevel(), "level 字段应写入并可查询");
    }

    @Test
    void testJoinQueryByCategoryName() {
        Category c = newCategory("数码-" + System.nanoTime(), 1);
        categoryService.save(c);

        productService.save(newProduct("iPhone-" + System.nanoTime(), "5999.00", 10, 5, c.getId()));
        productService.save(newProduct("iPad-" + System.nanoTime(), "3999.00", 20, 4, c.getId()));

        List<ProductCategoryVO> list = categoryService.listProductsByCategoryName(c.getName());
        assertEquals(2, list.size());
        ProductCategoryVO first = list.get(0);
        assertNotNull(first.getProductId());
        assertEquals(c.getName(), first.getCategoryName());
        assertEquals(c.getId(), first.getCategoryId());
        assertNotNull(first.getProductLevel());
    }

    @Test
    void testProductDetailWithCategory() {
        Category c = newCategory("家电-" + System.nanoTime(), 2);
        categoryService.save(c);

        Product p = newProduct("冰箱-" + System.nanoTime(), "2999.00", 5, 3, c.getId());
        productService.save(p);

        // level 字段从商品侧写入后应可查询
        Product loaded = productService.getById(p.getId());
        assertEquals(3, loaded.getLevel());

        ProductCategoryVO vo = categoryService.getProductDetail(p.getId());
        assertEquals(p.getName(), vo.getProductName());
        assertEquals(3, vo.getProductLevel());
        assertEquals(c.getId(), vo.getCategoryId());
        assertEquals(c.getName(), vo.getCategoryName());
        assertEquals(2, vo.getCategoryLevel());
    }

    @Test
    void testPageJoinQuery() {
        Category c = newCategory("图书-" + System.nanoTime(), 1);
        categoryService.save(c);

        for (int i = 0; i < 5; i++) {
            productService.save(newProduct("图书-" + System.nanoTime() + "-" + i, "99.00", 1, 2, c.getId()));
        }

        IPage<ProductCategoryVO> page = categoryService.pageProductsByCategoryName(c.getName(), 1, 2);
        assertTrue(page.getTotal() >= 5, "分页总数应不小于 5");
        assertEquals(2, page.getRecords().size(), "每页大小应为 2");
        assertEquals(1, page.getCurrent());
    }
}