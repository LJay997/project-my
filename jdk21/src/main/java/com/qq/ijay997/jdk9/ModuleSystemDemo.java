package com.qq.ijay997.jdk9;

import java.lang.module.ModuleDescriptor;

/**
 * JDK 9 —— 模块系统（JPMS）观测 Demo。
 *
 * <p>通过反射的 {@link Module} API 读取当前模块的信息，
 * 直观展示本模块的名称、导出包与依赖关系。</p>
 *
 * <p>运行方式（模块模式）：</p>
 * <pre>java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.ModuleSystemDemo</pre>
 *
 * @version JDK 9+
 */
public class ModuleSystemDemo {

    public static void main(String[] args) {
        // 当前类的模块（运行时会解析 module-info.java）
        Module myModule = ModuleSystemDemo.class.getModule();

        System.out.println("===== 当前模块信息 =====");
        System.out.println("模块名: " + myModule.getName());
        System.out.println("是否具名模块: " + myModule.isNamed());
        System.out.println("模块层: " + myModule.getLayer());

        // 导出包列表
        ModuleDescriptor descriptor = myModule.getDescriptor();
        System.out.println("\n----- 导出的包 -----");
        descriptor.exports().forEach(e -> System.out.println("  " + e.source()));

        System.out.println("\n----- 模块依赖（requires） -----");
        descriptor.requires().forEach(r -> System.out.println("  " + r.name()));

        // 底层平台模块（java.base）说明：模块化对 JDK 内部可见性的影响
        System.out.println("\n----- 说明 -----");
        System.out.println("java.base 模块自动 available，indexOfClass 等核心 API 从 Java 9 起强封封装。");
    }
}
