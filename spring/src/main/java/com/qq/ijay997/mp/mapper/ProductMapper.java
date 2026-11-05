package com.qq.ijay997.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qq.ijay997.mp.Level;
import com.qq.ijay997.mp.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品 Mapper：继承 BaseMapper 即可获得通用 CRUD 能力。
 *
 * @author ijay997
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * UNION ALL 演示：分别查询「已分类」与「未分类」商品，两个分支并集返回。
     * <p>两个分支都用别名 {@code p}，因此 {@link com.qq.ijay997.mp.interceptor.LevelInterceptor}
     * 会把 {@code p.level <= 阈值} 追加到<b>每个分支</b>的 WHERE 上（由
     * {@code LevelConditionProvider.LevelSelectVisitor} 遍历 {@code SetOperationList} 完成）。</p>
     * <p>分支条件互斥（{@code category_id IS NOT NULL} 与 {@code IS NULL}），
     * 用 {@code UNION ALL} 不会有重复行，也不依赖去重排序。</p>
     */
    @Level("p")
    @Select("SELECT p.* FROM mp_product p WHERE p.deleted = 0 AND p.category_id IS NOT NULL " +
            "UNION ALL " +
            "SELECT p.* FROM mp_product p WHERE p.deleted = 0 AND p.category_id IS NULL")
    List<Product> selectClassifiedAndUnclassified();
}