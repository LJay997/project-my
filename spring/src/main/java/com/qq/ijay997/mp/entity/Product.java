package com.qq.ijay997.mp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（MyBatis-Plus 演示），使用了：
 * - 雪花主键 {@link TableId#type()} = ASSIGN_ID
 * - 乐观锁 {@link Version}
 * - 逻辑删除 {@link TableLogic}
 * - 字段自动填充 {@link TableField#fill()}
 *
 * @author ijay997
 */
@Data
@TableName("mp_product")
public class Product {

    /**
     * 主键（雪花算法，MyBatis-Plus 自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 商品等级（如 1~5，用于排序/区间过滤）。
     * 选择 Integer 而非 String：等级是数值语义，便于 BETWEEN/排序比较，且支持 null 表示“未评级”，占用更小。
     */
    private Integer level;

    /**
     * 所属分类 ID（外键，关联 {@code mp_category.id}）
     */
    private Long categoryId;

    /**
     * 乐观锁版本号：更新时自动 +1，防止并发覆盖
     */
    @Version
    private Integer version = 0;

    /**
     * 逻辑删除标记：0=未删除，1=已删除
     */
    @TableLogic
    private Integer deleted = 0;

    /**
     * 创建时间：插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间：插入与更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}