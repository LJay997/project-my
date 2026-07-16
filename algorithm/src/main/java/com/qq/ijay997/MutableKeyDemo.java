package com.qq.ijay997;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MutableKeyDemo {

    static class MutableKey {
        private String name;

        public MutableKey(String name) {
            this.name = name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MutableKey that = (MutableKey) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public static void main(String[] args) {
        MutableKey key = new MutableKey("hello");
        Map<MutableKey, String> map = new HashMap<>();
        map.put(key, "world");

        System.out.println("修改前 hashCode: " + key.hashCode());
        System.out.println("修改前取值: " + map.get(key)); // world ✅

        key.setName("hello!!!");

        System.out.println("修改后 hashCode: " + key.hashCode());
        System.out.println("修改后取值: " + map.get(key)); // null ❌ 找不到了！

        System.out.println("\n=== 为什么找不到？===");
        System.out.println("put 时 hash = " + Objects.hash("hello") + "，存在桶 A");
        System.out.println("get 时 hash = " + Objects.hash("hello!!!") + "，去桶 B 找");
        System.out.println("桶 B 是空的 → 返回 null");
    }
}