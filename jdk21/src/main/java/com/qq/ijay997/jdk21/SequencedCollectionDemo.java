package com.qq.ijay997.jdk21;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;

/**
 * JDK 21 —— SequencedCollection 顺序集合接口 Demo（正式特性）。
 *
 * <p>新增统一接口，用一致的方式取首/尾、反序视图：
 * {@code getFirst / getLast / addFirst / addLast / reversed}。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk21.SequencedCollectionDemo</p>
 *
 * @version JDK 21+
 */
public class SequencedCollectionDemo {

    public static void main(String[] args) {
        // ArrayList 也实现了 SequencedCollection
        SequencedCollection<String> list = new ArrayList<>(List.of("a", "b", "c"));

        System.out.println("原始    : " + list);
        System.out.println("getFirst: " + list.getFirst() + "   getLast: " + list.getLast());

        // 反序视图（不改变原集合）
        var reversed = list.reversed();
        System.out.println("reversed: " + reversed);

        // 在首尾添加
        list.addFirst("first");
        list.addLast("last");
        System.out.println("addFirst/addLast 后: " + list);

        // 顺序保持的类型也可用于 Set / Deque
        var set = new LinkedHashSet<String>();
        set.add("x");
        set.add("y");
        System.out.println("LinkedHashSet getFirst=" + set.getFirst() + " getLast=" + set.getLast());
    }
}
