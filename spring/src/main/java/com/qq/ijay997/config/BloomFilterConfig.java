package com.qq.ijay997.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 布隆过滤器配置类
 * 用于防止缓存穿透，快速判断用户ID是否存在
 */
@Component
public class BloomFilterConfig {

    private final RedissonClient redissonClient;
    
    // 布隆过滤器名称
    private static final String BLOOM_FILTER_NAME = "user_bloom_filter";
    
    // 预期插入的元素数量
    private static final long EXPECTED_INSERTIONS = 100000L;
    
    // 误判率（0.03 = 3%）
    private static final double FALSE_POSITIVE_PROBABILITY = 0.03;
    
    // 布隆过滤器实例
    private RBloomFilter<Long> userBloomFilter;

    public BloomFilterConfig(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 初始化布隆过滤器
     * 在 Spring 容器启动时自动执行
     */
    @PostConstruct
    public void init() {
        userBloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        
        // 初始化布隆过滤器
        // expectedInsertions: 预期插入的元素数量
        // falseProbability: 允许的误判率（越小越准确，但占用空间越大）
        userBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_POSITIVE_PROBABILITY);
        
        System.out.println(String.format(
            "布隆过滤器初始化完成 - 名称: %s, 预期容量: %d, 误判率: %.2f%%",
            BLOOM_FILTER_NAME, EXPECTED_INSERTIONS, FALSE_POSITIVE_PROBABILITY * 100
        ));
    }

    /**
     * 向布隆过滤器中添加元素
     * 
     * @param userId 用户ID
     * @return 添加前的状态（false表示首次添加，true表示可能已存在）
     */
    public boolean addUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userBloomFilter.add(userId);
    }

    /**
     * 批量添加用户ID到布隆过滤器
     * 
     * @param userIds 用户ID列表
     */
    public void addUsers(Iterable<Long> userIds) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            if (userId != null) {
                userBloomFilter.add(userId);
            }
        }
    }

    /**
     * 判断用户ID是否可能存在
     * 
     * @param userId 用户ID
     * @return true=可能存在（需要查数据库确认），false=一定不存在（可直接返回）
     */
    public boolean mightContain(Long userId) {
        if (userId == null) {
            return false;
        }
        return userBloomFilter.contains(userId);
    }

    /**
     * 获取布隆过滤器中已添加的元素数量（估算值）
     * 
     * @return 元素数量
     */
    public long count() {
        return userBloomFilter.count();
    }

    /**
     * 清空布隆过滤器（谨慎使用）
     */
    public void clear() {
        userBloomFilter.delete();
        // 重新初始化
        userBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_POSITIVE_PROBABILITY);
        System.out.println("布隆过滤器已清空并重新初始化");
    }
}
