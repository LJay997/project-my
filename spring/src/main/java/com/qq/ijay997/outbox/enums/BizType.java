package com.qq.ijay997.outbox.enums;

/**
 * 触发远端调用的业务场景类型。
 *
 * @author ijay997
 */
public enum BizType {

    /** 文件导入 */
    IMPORT("文件导入"),

    /** 数据新建 */
    CREATE("数据新建"),

    /** 数据编辑 */
    UPDATE("数据编辑"),

    /** 数据删除 */
    DELETE("数据删除");

    private final String desc;

    BizType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}