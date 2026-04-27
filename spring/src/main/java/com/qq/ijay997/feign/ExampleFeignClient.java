package com.qq.ijay997.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 示例 Feign Client - 调用外部 REST API
 * 
 * 使用方式：
 * 1. 注入此接口：@Autowired private ExampleFeignClient exampleFeignClient;
 * 2. 调用方法：exampleFeignClient.getPost(1);
 * 
 * @author ijay997
 */
@FeignClient(name = "jsonPlaceholder", url = "https://jsonplaceholder.typicode.com")
public interface ExampleFeignClient {

    /**
     * GET 请求示例 - 获取文章
     * 
     * @param id 文章ID
     * @return 文章数据
     */
    @GetMapping("/posts/{id}")
    Map<String, Object> getPost(@PathVariable("id") Long id);

    /**
     * GET 请求示例 - 获取所有文章
     * 
     * @return 文章列表
     */
    @GetMapping("/posts")
    java.util.List<Map<String, Object>> getPosts();

    /**
     * POST 请求示例 - 创建文章
     * 
     * @param body 请求体
     * @return 创建结果
     */
    @PostMapping("/posts")
    Map<String, Object> createPost(@RequestBody Map<String, Object> body);
}
