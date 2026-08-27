package com.qq.ijay997;

import redis.clients.jedis.Jedis;
/*<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>5.1.2</version>
</dependency>*/
public class JedisDemo {
    public static void main(String[] args) {
        Jedis jedis = null;
        try {
            // ① 创建 Jedis 对象（指定 Redis 服务器地址和端口）
            jedis = new Jedis("localhost", 6379);

            // ② 认证（如果 Redis 设置了密码，必须调用 auth 进行身份验证）
//            jedis.auth("your_password");

            // ③ 建立连接验证（通过 ping 命令测试连接是否成功，返回 PONG 表示连接正常）
            String pong = jedis.ping();
            System.out.println("连接成功：" + pong);

            // ④ 执行业务操作（示例：设置和获取键值对）
            jedis.set("name", "Jedis测试");
            String value = jedis.get("name");
            System.out.println("获取的值：" + value);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // ⑤ 断开连接，释放资源（必须放在 finally 块中确保一定执行）
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}