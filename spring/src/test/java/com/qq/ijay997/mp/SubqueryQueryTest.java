package com.qq.ijay997.mp;

import com.qq.ijay997.auth.UserContextHolder;
import com.qq.ijay997.mp.service.CategoryService;
import com.qq.ijay997.mp.vo.ProductCategoryVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 子查询集成测试：验证「价格高于所属分类商品均价」的关联子查询，
 * 以及 {@link LevelInterceptor} 对该查询的实际影响（仅改写外层、不改写子查询）。
 *
 * @author ijay997
 */
@SpringBootTest
class SubqueryQueryTest {

    @Autowired
    private CategoryService categoryService;

    /**
     * 不加权限阈值时：返回结果中每条商品价格都应高于其分类均价；
     * 种子分类 5「智能穿戴」中仅 9999 的蓝牙眼镜(114)高于均价。
     */
    @Test
    void testAboveCategoryAvgPrice() {
        List<ProductCategoryVO> list = categoryService.listProductsAboveCategoryAvgPrice();

        assertFalse(list.isEmpty(), "应能查询到高于分类均价的商品");
        for (ProductCategoryVO vo : list) {
            assertNotNull(vo.getCategoryAvgPrice(), "子查询应填充分类均价");
            assertTrue(vo.getPrice().compareTo(vo.getCategoryAvgPrice()) > 0,
                    "商品价格应高于所属分类均价: " + vo.getProductName());
        }

        assertTrue(containsProduct(list, 114L), "蓝牙眼镜(9999) 应高于分类 5 均价");
        assertFalse(containsProduct(list, 112L), "智能手表(1999) 应低于分类 5 均价");
        assertFalse(containsProduct(list, 113L), "智能手环(199) 应低于分类 5 均价");
        assertTrue(containsProduct(list, 103L), "笔记本电脑(8999) 应高于分类 1 均价");
    }

    /**
     * 设置权限阈值后：{@code @Level("p")} 只改写了外层查询（p.level <= 4），
     * 子查询里的 {@code AVG} 仍按全量计算（权限穿透），因此分类 5 的智能手表(1999)
     * 依然低于「包含 level=5 蓝牙眼镜」的均价 4065.67，不会出现在结果中。
     */
    @Test
    void testLevelInterceptorOnlyRewritesOuterQuery() {
        try {
            UserContextHolder.setLevel(4);
            List<ProductCategoryVO> list = categoryService.listProductsAboveCategoryAvgPrice();

            for (ProductCategoryVO vo : list) {
                assertTrue(vo.getProductLevel() <= 4, "外层 p.level <= 4 应生效: " + vo.getProductName());
            }
            assertFalse(containsProduct(list, 114L), "level=5 的蓝牙眼镜应被外层过滤");

            // 权限穿透：若子查询也被过滤，分类 5 均价会变成 (1999+199)/2=1099，智能手表本应出现；
            // 实际子查询未被改写，均价仍是 4065.67，故智能手表不出现。
            assertFalse(containsProduct(list, 112L),
                    "子查询未被 LevelInterceptor 改写，均价仍包含高等级商品");
        } finally {
            UserContextHolder.clear();
        }
    }

    private boolean containsProduct(List<ProductCategoryVO> list, Long productId) {
        return list.stream()
                .anyMatch(vo -> Long.valueOf(productId).equals(vo.getProductId()));
    }
}
