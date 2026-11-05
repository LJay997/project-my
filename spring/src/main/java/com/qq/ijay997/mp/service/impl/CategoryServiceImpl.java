package com.qq.ijay997.mp.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qq.ijay997.mp.entity.Category;
import com.qq.ijay997.mp.mapper.CategoryMapper;
import com.qq.ijay997.mp.service.CategoryService;
import com.qq.ijay997.mp.vo.ProductCategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务实现。
 *
 * @author ijay997
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<ProductCategoryVO> listProductsByCategoryName(String name) {
        return baseMapper.selectProductsByCategoryName(name);
    }

    @Override
    public IPage<ProductCategoryVO> pageProductsByCategoryName(String name, long current, long size) {
        Page<ProductCategoryVO> page = new Page<>(current, size);
        return baseMapper.selectProductsByCategoryNamePage(page, name);
    }

    @Override
    public ProductCategoryVO getProductDetail(Long productId) {
        return baseMapper.selectProductDetail(productId);
    }

    @Override
    public List<ProductCategoryVO> listProductsAboveCategoryAvgPrice() {
        return baseMapper.selectProductsAboveCategoryAvgPrice();
    }
}