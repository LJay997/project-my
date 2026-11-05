package com.qq.ijay997.mp.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品与分类关联查询结果 VO（Product 关键字段 + Category 字段）。
 * 注意：此处字段不含敏感信息，无需接入 @Sensitive 脱敏链路。
 *
 * @author ijay997
 */
@Data
public class ProductCategoryVO {

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品价格 */
    private BigDecimal price;

    /** 商品库存 */
    private Integer stock;

    /** 商品等级 */
    private Integer productLevel;

    /** 分类 ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 分类层级 */
    private Integer categoryLevel;

    /**
     * 所属分类的商品均价（仅「高于分类均价」子查询会填充，其它关联查询为 null）。
     */
    private BigDecimal categoryAvgPrice;
}