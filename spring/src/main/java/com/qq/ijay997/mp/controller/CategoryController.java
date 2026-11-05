package com.qq.ijay997.mp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qq.ijay997.mp.entity.Category;
import com.qq.ijay997.mp.service.CategoryService;
import com.qq.ijay997.mp.vo.ProductCategoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类控制器（分类 CRUD + 商品关联查询演示）。
 *
 * @author ijay997
 */
@RestController
@RequestMapping("/api/mp/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类（level 字段演示写入）。
     */
    @PostMapping
    public Category create(@RequestBody Category category) {
        categoryService.save(category);
        return category;
    }

    /**
     * 从分类侧查商品：按分类名查商品列表。
     */
    @GetMapping("/products")
    public List<ProductCategoryVO> listProducts(@RequestParam String name) {
        return categoryService.listProductsByCategoryName(name);
    }

    /**
     * 从分类侧查商品：分页（关联查询配合分页插件）。
     */
    @GetMapping("/products/page")
    public IPage<ProductCategoryVO> pageProducts(@RequestParam String name,
                                                 @RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "5") long size) {
        return categoryService.pageProductsByCategoryName(name, current, size);
    }

    /**
     * 从商品侧查关联信息：按商品 ID 查详情（带分类）。
     */
    @GetMapping("/product/{productId}/detail")
    public ProductCategoryVO productDetail(@PathVariable Long productId) {
        return categoryService.getProductDetail(productId);
    }

    /**
     * 子查询演示：查询价格高于所属分类商品均价的商品。
     */
    @GetMapping("/products/above-avg")
    public List<ProductCategoryVO> productsAboveCategoryAvg() {
        return categoryService.listProductsAboveCategoryAvgPrice();
    }
}