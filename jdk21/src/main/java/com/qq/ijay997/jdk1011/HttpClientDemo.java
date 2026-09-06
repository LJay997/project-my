package com.qq.ijay997.jdk1011;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import com.sun.net.httpserver.HttpServer;

/**
 * JDK 11 —— 标准 HTTP 客户端 HttpClient Demo。
 *
 * <p>展示同步（{@code send}）与异步（{@code sendAsync}）两种请求方式。
 * 为避免依赖外网，Demo 用 JDK 内置 {@link HttpServer} 自建一个回环服务端。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1011.HttpClientDemo</p>
 *
 * @version JDK 11+
 */
public class HttpClientDemo {

    public static void main(String[] args) throws Exception {
        // 启动内置 HTTP 服务器，监听随机可用端口，响应固定文本
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/hello", exchange -> {
            byte[] body = "Hello from built-in server".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        int port = server.getAddress().getPort();
        String url = "http://127.0.0.1:" + port + "/hello";
        System.out.println("回环服务端 URL: " + url);

        // 创建客户端
        HttpClient client = HttpClient.newBuilder().build();

        // 1) 同步请求
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> syncResp = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("同步请求: 状态=" + syncResp.statusCode() + " 响应=" + syncResp.body());

        // 2) 异步请求
        CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> asyncResp = future.join();
        System.out.println("异步请求: 状态=" + asyncResp.statusCode() + " 响应=" + asyncResp.body());

        server.stop(0);
        System.out.println("已关闭服务端。");
    }
}
