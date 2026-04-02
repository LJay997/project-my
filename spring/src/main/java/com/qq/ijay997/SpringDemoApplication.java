package com.qq.ijay997;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 演示应用主类
 * 
 * @author ijay997
 */
@SpringBootApplication
public class SpringDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
        System.out.println("=================================");
        System.out.println("Spring Boot 演示应用启动成功!");
        System.out.println("访问地址：http://localhost:8080");
        System.out.println("=================================");
    }
}
