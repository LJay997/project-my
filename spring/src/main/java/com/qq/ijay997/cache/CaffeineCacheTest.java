package com.qq.ijay997.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheTest {
    public static void main(String[] args) {
        System.out.println("===== Caffeine 缓存测试开始 =====\n");

//        testBasicCache();
//        testCacheWithExpiration();
//        testCacheWithSizeLimit();
//        testCacheWithRemovalListener();
        System.out.println(System.currentTimeMillis() * 1_000_000_000L - System.nanoTime() );
//        testAsyncCache();

        System.out.println("\n===== Caffeine 缓存测试结束 =====");
    }

    private static void testBasicCache() {
        System.out.println("1. 基本缓存操作测试");
        Cache<String, String> cache = Caffeine.newBuilder()
                .build();

        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        System.out.println("   - 添加 key1=value1, key2=value2, key3=value3");
        System.out.println("   - 获取 key1: " + cache.getIfPresent("key1"));
        System.out.println("   - 获取 key2: " + cache.getIfPresent("key2"));
        System.out.println("   - 获取 key4 (不存在): " + cache.getIfPresent("key4"));

        String value = cache.get("key4", k -> {
            System.out.println("   - key4 不存在，执行加载逻辑");
            return "value4";
        });
        System.out.println("   - 使用 get 方法获取 key4: " + value);

        cache.invalidate("key1");
        System.out.println("   - 移除 key1 后，获取 key1: " + cache.getIfPresent("key1"));

        System.out.println("   - 当前缓存大小: " + cache.estimatedSize());
        System.out.println();
    }

    private static void testCacheWithExpiration() {
        System.out.println("2. 基于时间的过期策略测试");

        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.SECONDS)
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build();

        cache.put("expKey1", "expValue1");
        System.out.println("   - 添加 expKey1=expValue1 (写入后 2 秒过期，访问后 1 秒过期)");
        System.out.println("   - 立即获取 expKey1: " + cache.getIfPresent("expKey1"));

        try {
            Thread.sleep(1500);
            System.out.println("   - 等待 1.5 秒后获取 expKey1: " + cache.getIfPresent("expKey1"));

            Thread.sleep(1000);
            System.out.println("   - 再等待 1 秒后获取 expKey1: " + cache.getIfPresent("expKey1"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private static void testCacheWithSizeLimit() {
        System.out.println("3. 基于大小的淘汰策略测试");

        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(3)
                .build();

        cache.put("sizeKey1", "value1");
        cache.put("sizeKey2", "value2");
        cache.put("sizeKey3", "value3");
        System.out.println("   - 添加 3 个元素 (最大容量为 3)");
        System.out.println("   - 当前缓存大小: " + cache.estimatedSize());

        cache.put("sizeKey4", "value4");
        System.out.println("   - 添加第 4 个元素 sizeKey4");
        System.out.println("   - 当前缓存大小: " + cache.estimatedSize());
        System.out.println("   - sizeKey1 是否存在: " + (cache.getIfPresent("sizeKey1") != null));
        System.out.println("   - sizeKey4 是否存在: " + (cache.getIfPresent("sizeKey4") != null));

        Cache<String, String> weightCache = Caffeine.newBuilder()
                .maximumWeight(10)
                .weigher((String key, String value) -> value.length())
                .build();

        weightCache.put("weightKey1", "12345");
        weightCache.put("weightKey2", "67890");
        System.out.println("\n   - 基于权重的缓存 (最大权重 10，权重=值长度)");
        System.out.println("   - 添加 weightKey1='12345' (权重5), weightKey2='67890' (权重5)");

        weightCache.put("weightKey3", "abc");
        System.out.println("   - 添加 weightKey3='abc' (权重3)，总权重将超过限制");
        System.out.println("   - 当前缓存大小: " + weightCache.estimatedSize());
        System.out.println();
    }

    private static void testCacheWithRemovalListener() {
        System.out.println("4. 移除监听器测试");

        RemovalListener<String, String> listener = (key, value, cause) -> {
            System.out.println("   - 监听到移除事件: key=" + key + ", value=" + value + ", 原因=" + cause);
        };

        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(2)
                .removalListener(listener)
                .build();

        cache.put("listenKey1", "value1");
        cache.put("listenKey2", "value2");
        System.out.println("   - 添加 listenKey1 和 listenKey2");

        cache.put("listenKey3", "value3");
        System.out.println("   - 添加 listenKey3 (触发淘汰)");

        cache.invalidate("listenKey2");
        System.out.println("   - 手动移除 listenKey2");

        System.out.println();
    }

    /**
     * nanoTime → 墙钟纳秒偏移量（类加载时一次性计算）：
     * 任意 Caffeine currentTime 加上此偏移即可得到对应的墙钟纪元纳秒，
     * 避免每次 expireAfterCreate 都做减法 + 创建 Instant/Duration 对象。
     */
    private static final long NANO_TO_WALL_OFFSET;
    static {
        long nanoBase = System.nanoTime();
        Instant wallBase = Instant.now();
        NANO_TO_WALL_OFFSET = wallBase.getEpochSecond() * 1_000_000_000L + wallBase.getNano() - nanoBase;
    }

    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");

    /**
     * 将 Caffeine 的 nanoTime 转换为真实墙钟时间
     * 利用预计算的偏移量，一次加法即可得到墙钟纪元纳秒
     */
    private static Instant nanoTimeToInstant(long nanoTime) {
        long wallNanos = nanoTime + NANO_TO_WALL_OFFSET;
        return Instant.ofEpochSecond(wallNanos / 1_000_000_000L, wallNanos % 1_000_000_000L);
    }

    private static void testAsyncCache() {
        System.out.println("5. 自定义过期策略测试 (Expiry 接口)");

        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, String>() {
                    @Override
                    public long expireAfterCreate(String key, String value, long currentTime) {
                        // 将 Caffeine 的 nanoTime 转换为真实墙钟时间
                        Instant nowWallClock = nanoTimeToInstant(currentTime);
                        // 用推算出的时间获取当前日期（确保与过期时间使用同一时区）
                        LocalDate today = nowWallClock.atZone(ZONE_SHANGHAI).toLocalDate();
                        // 计算明天 00:00:00 的时间点
                        Instant tomorrowMidnight = today.plusDays(1).atStartOfDay(ZONE_SHANGHAI).toInstant();
                        // 返回到午夜的剩余纳秒数（≥0 保护，防止边界情况返回负数）
                        return Math.max(0, Duration.between(nowWallClock, tomorrowMidnight).toNanos());
                    }

                    @Override
                    public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
        cache.get("short_key",
                k ->
                        "短期缓存");
//        cache.put("short_key", "短期缓存");
        cache.put("long_key", "长期缓存");
        System.out.println("   - 添加 short_key (1秒过期) 和 long_key (5秒过期)");

        try {
            Thread.sleep(1500);
            System.out.println("   - 等待 1.5 秒后:");
            System.out.println("     short_key 存在: " + (cache.getIfPresent("short_key") != null));
            System.out.println("     long_key 存在: " + (cache.getIfPresent("long_key") != null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println();

        System.out.println("6. 统计信息测试");
        Cache<String, String> statsCache = Caffeine.newBuilder()
                .maximumSize(100)
                .recordStats()
                .build();

        statsCache.put("statsKey1", "value1");
        statsCache.getIfPresent("statsKey1");
        statsCache.getIfPresent("statsKey2");
        statsCache.get("statsKey3", k -> "computedValue");

        System.out.println("   - 命中次数: " + statsCache.stats().hitCount());
        System.out.println("   - 未命中次数: " + statsCache.stats().missCount());
        System.out.println("   - 命中率: " + String.format("%.2f%%", statsCache.stats().hitRate() * 100));
        System.out.println("   - 加载次数: " + statsCache.stats().loadCount());
        System.out.println("   - 淘汰次数: " + statsCache.stats().evictionCount());
    }
}