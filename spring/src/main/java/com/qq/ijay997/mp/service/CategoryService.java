package com.qq.ijay997.mp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qq.ijay997.mp.entity.Category;
import com.qq.ijay997.mp.vo.ProductCategoryVO;

import java.util.List;

/**
 * 商品分类服务：继承 MyBatis-Plus 的 {@link IService} 获得分类 CRUD，并对外提供与商品的关联查询。
 *
 * @author ijay997
 */
public interface CategoryService extends IService<Category> {

    /**
     * 从分类侧查商品（按分类名，非分页）。
     */
    List<ProductCategoryVO> listProductsByCategoryName(String name);

    /**
     * 从分类侧查商品（按分类名，分页）。
     */
    IPage<ProductCategoryVO> pageProductsByCategoryName(String name, long current, long size);

    /**
     * 从商品侧查关联信息（按商品 ID 查详情，带分类）。
     */
    ProductCategoryVO getProductDetail(Long productId);

    /**
     * 子查询演示：查询价格高于所属分类商品均价的商品（带分类信息与分类均价）。
     */
    List<ProductCategoryVO> listProductsAboveCategoryAvgPrice();
}