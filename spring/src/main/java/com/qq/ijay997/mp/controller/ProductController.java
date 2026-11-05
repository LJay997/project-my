package com.qq.ijay997.mp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qq.ijay997.mp.entity.Product;
import com.qq.ijay997.mp.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品控制器（MyBatis-Plus 完整 CRUD 演示）。
 *
 * @author ijay997
 */
@RestController
@RequestMapping("/api/mp/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 新增商品。
     */
    @PostMapping
    public Product create(@RequestBody Product product) {
        productService.save(product);
        return product;
    }

    /**
     * 根据 ID 查询（逻辑删除的记录不会被查询到）。
     */
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    /**
     * 更新商品（演示乐观锁版本号自增）。
     */
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productService.updateById(product);
        return productService.getById(id);
    }

    /**
     * 逻辑删除商品。
     */
    @DeleteMapping("/{id}")
    public Boolean delete(@PathVariable Long id) {
        return productService.removeById(id);
    }

    /**
     * 分页查询（支持最低价格过滤）。
     */
    @GetMapping("/page")
    public IPage<Product> page(@RequestParam(defaultValue = "1") long current,
                               @RequestParam(defaultValue = "5") long size,
                               @RequestParam(required = false) BigDecimal minPrice) {
        return productService.pageByPrice(minPrice, current, size);
    }

    /**
     * 条件构造器查询（名称模糊 + 价格区间）。
     */
    @GetMapping("/condition")
    public List<Product> condition(@RequestParam(required = false) String name,
                                   @RequestParam(required = false) BigDecimal minPrice,
                                   @RequestParam(required = false) BigDecimal maxPrice) {
        return productService.listByCondition(name, minPrice, maxPrice);
    }
}