package com.qq.ijay997.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qq.ijay997.mp.Level;
import com.qq.ijay997.mp.entity.Category;
import com.qq.ijay997.mp.vo.ProductCategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品分类 Mapper：继承 BaseMapper 获得 CRUD，并通过自定义 SQL 实现与 {@code mp_product} 的关联查询。
 * <p>MyBatis-Plus 不支持 JOIN，故用 {@link Select} 手写 SQL，列别名配下划线，由 map-underscore-to-camel-case 映射到 VO。</p>
 *
 * @author ijay997
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    String JOIN_SELECT = "SELECT p.id AS product_id, p.name AS product_name, p.price AS price, " +
            "p.stock AS stock, p.level AS product_level, p.category_id AS category_id, " +
            "c.name AS category_name, c.level AS category_level " +
            "FROM mp_product p LEFT JOIN mp_category c ON p.category_id = c.id ";

    /**
     * 从分类侧查询：按分类名查商品列表（非分页）。
     */
    @Select(JOIN_SELECT + "WHERE c.name = #{name} AND p.deleted = 0 ORDER BY p.id ASC")
    List<ProductCategoryVO> selectProductsByCategoryName(@Param("name") String name);

    /**
     * 从分类侧查询：按分类名查商品，分页（配合 PaginationInnerInterceptor）。
     * 分页方法首个参数传 {@link Page}，MyBatis-Plus 会自动生成 count 与 limit。
     */
    @Level("p")
//    @Select(JOIN_SELECT + "WHERE c.name = #{name} AND p.deleted = 0 ORDER BY p.id ASC")
    @Select(JOIN_SELECT + "WHERE p.deleted = 0 ORDER BY p.id ASC")
    IPage<ProductCategoryVO> selectProductsByCategoryNamePage(Page<ProductCategoryVO> page, @Param("name") String name);

    /**
     * 从商品侧查询：按商品 ID 查详情（含分类信息）。
     */
    @Select(JOIN_SELECT + "WHERE p.id = #{id} AND p.deleted = 0")
    ProductCategoryVO selectProductDetail(@Param("id") Long id);

    /**
     * 子查询演示：查询「价格高于所属分类商品均价」的商品，并返回其分类均价。
     * <p>用关联子查询 {@code p.price > (SELECT AVG(p2.price) ... WHERE p2.category_id = p.category_id)}，
     * 未分类商品（category_id 为 null）不参与。</p>
     * <p><b>注意：</b>{@code @Level("p")} 只会让 {@code LevelInterceptor} 改写<b>外层</b>查询，
     * 子查询 {@code p2} 不会被改写（见 MYBATIS_PLUS_GUIDE.md 的局限说明）。</p>
     */
    @Level("p")
    @Select("SELECT p.id AS product_id, p.name AS product_name, p.price AS price, " +
            "p.stock AS stock, p.level AS product_level, p.category_id AS category_id, " +
            "c.name AS category_name, c.level AS category_level, " +
            "(SELECT AVG(p2.price) FROM mp_product p2 WHERE p2.category_id = p.category_id AND p2.deleted = 0) AS category_avg_price " +
            "FROM mp_product p LEFT JOIN mp_category c ON p.category_id = c.id " +
            "WHERE p.deleted = 0 AND p.category_id IS NOT NULL " +
            "AND p.price > (SELECT AVG(p3.price) FROM mp_product p3 WHERE p3.category_id = p.category_id AND p3.deleted = 0) " +
            "ORDER BY p.id ASC")
    List<ProductCategoryVO> selectProductsAboveCategoryAvgPrice();
}