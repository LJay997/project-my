package com.qq.ijay997.dubbo;

import com.qq.ijay997.dubbo.api.UserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dubbo 服务测试
 * 
 * @author ijay997
 */
@SpringBootTest
class DubboServiceTest {

    @DubboReference
    private UserService userService;

    @Test
    void testGetUserNameById() {
        String userName = userService.getUserNameById(123L);
        assertNotNull(userName);
        assertEquals("用户_123", userName);
        System.out.println("测试结果：" + userName);
    }

    @Test
    void testSayHello() {
        String result = userService.sayHello("张三");
        assertNotNull(result);
        assertTrue(result.contains("张三"));
        System.out.println("测试结果：" + result);
    }

    @Test
    void testAdd() {
        int result = userService.add(10, 20);
        assertEquals(30, result);
        System.out.println("测试结果：" + result);
    }
}
