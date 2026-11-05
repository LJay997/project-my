package com.qq.ijay997.mp.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类实体（MyBatis-Plus 演示）。
 *
 * @author ijay997
 */
@Data
@TableName("mp_category")
public class Category {

    /**
     * 主键（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类层级（如 1=一级分类，2=二级分类）。
     * 选择 Integer：层级是数值语义，便于排序/比较，且占用小、支持 null。
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer level;

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