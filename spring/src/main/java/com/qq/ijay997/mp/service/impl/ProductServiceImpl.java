package com.qq.ijay997.mp.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qq.ijay997.mp.entity.Product;
import com.qq.ijay997.mp.mapper.ProductMapper;
import com.qq.ijay997.mp.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务实现。
 *
 * @author ijay997
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public IPage<Product> pageByPrice(BigDecimal minPrice, long current, long size) {
        Page<Product> page = new Page<>(current, size);
        return this.lambdaQuery()
                .ge(minPrice != null, Product::getPrice, minPrice)
                .orderByAsc(Product::getPrice)
                .page(page);
    }

    @Override
    public List<Product> listByCondition(String name, BigDecimal minPrice, BigDecimal maxPrice) {
        return this.lambdaQuery()
                .like(StringUtils.hasText(name), Product::getName, name)
                .ge(minPrice != null, Product::getPrice, minPrice)
                .le(maxPrice != null, Product::getPrice, maxPrice)
                .orderByAsc(Product::getId)
                .list();
    }
}