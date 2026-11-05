package com.qq.ijay997.mp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qq.ijay997.mp.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper：继承 BaseMapper 即可获得通用 CRUD 能力。
 *
 * @author ijay997
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}