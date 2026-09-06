/**
 * JDK 9 模块系统（JPMS）演示模块。
 *
 * <p>本模块承载 JDK 8 → 17 以及 JDK 21 各版本特性的代码演示。
 * 属于模块化源布局；所有 demo 类均可用如下命令以模块方式运行：</p>
 * <pre>
 * java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.CollectionFactoryDemo
 * </pre>
 *
 * @version JDK 9+
 */
module jdk21demos {
    // 需要 HTTP 客户端模块（JDK 11+），供 jdk1011.HttpClientDemo 使用
    requires java.net.http;
    // 需要内置 HTTP 服务器模块（用于自建回环服务端，避免外网依赖）
    requires jdk.httpserver;

    // 导出各版本演示包
    exports com.qq.ijay997;
    exports com.qq.ijay997.jdk9;
    exports com.qq.ijay997.jdk1011;
    exports com.qq.ijay997.jdk1415;
    exports com.qq.ijay997.jdk1617;
    exports com.qq.ijay997.jdk21;
}
