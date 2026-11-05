package com.qq.ijay997.mp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qq.ijay997.mp.entity.Product;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务接口：继承 MyBatis-Plus 的 {@link IService} 获得通用 CRUD 与分页能力。
 *
 * @author ijay997
 */
public interface ProductService extends IService<Product> {

    /**
     * 按最低价格分页查询。
     */
    IPage<Product> pageByPrice(BigDecimal minPrice, long current, long size);

    /**
     * 条件构造器示例：按名称模糊 + 价格区间查询。
     */
    List<Product> listByCondition(String name, BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * UNION ALL 示例：已分类商品与未分类商品的并集。
     * <p>Mapper 方法带 {@code @Level("p")}，{@code LevelInterceptor} 会给 UNION 的每个分支
     * 各自追加 {@code p.level <= 阈值}，演示多分支 SQL 的权限改写。</p>
     */
    List<Product> listClassifiedAndUnclassified();
}