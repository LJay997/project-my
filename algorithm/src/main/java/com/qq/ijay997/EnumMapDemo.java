package com.qq.ijay997;

import java.util.EnumMap;
import java.util.Map;

public class EnumMapDemo {
    enum Status {
        CREATED(1, "订单已创建"),
        PROCESSING(2, "处理中"),
        COMPLETED(3, "已完成"),
        CANCELLED(4, "已取消");

        private final int code;
        private final String description;

        Status(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        // 通过 code 查找枚举
        public static Status fromCode(int code) {
            for (Status status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code: " + code);
        }    }

    public static void main(String[] args) {
        // 创建时必须指定枚举类的 Class 对象
        Map<Status, String> statusMap = new EnumMap<>(Status.class);

        // 放入数据
        statusMap.put(Status.CREATED, "订单已创建");
        statusMap.put(Status.PROCESSING, "处理中");
        statusMap.put(Status.COMPLETED, "已完成");
        // statusMap.put(null, "Error"); // 编译报错或运行抛 NullPointerException (Key不能为null)

        // 获取数据 (O(1))
        System.out.println(statusMap.get(Status.CREATED)); 

        // 遍历：永远按照枚举定义的顺序 (CREATED -> PROCESSING -> COMPLETED -> CANCELLED)
        for (Map.Entry<Status, String> entry : statusMap.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // 使用自定义 code，不依赖 ordinal
        System.out.println("\n使用自定义 code:");
        for (Status status : Status.values()) {
            System.out.println(status.getCode() + ": " + status.getDescription());
        }
    }
}