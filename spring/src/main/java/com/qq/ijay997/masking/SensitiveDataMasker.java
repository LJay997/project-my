package com.qq.ijay997.masking;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 文件导出场景的敏感数据处理器。
 * <p>
 * 在 EasyExcel 写出前，反射遍历 DTO 的 {@link Sensitive} 字段：
 * 当前用户无对应权限时，把字段值改写为脱敏值。与 JSON 场景复用完全相同的注解和规则。
 * <p>
 * 仅处理目标对象自身声明的 String 字段，不递归处理嵌套对象（导出模型为扁平结构）。
 *
 * @author ijay997
 */
@Component
public class SensitiveDataMasker {

    private final ConcurrentMap<Class<?>, List<Field>> sensitiveFieldCache = new ConcurrentHashMap<>();

    private static final LoadingCache<Class<?>, List<Field>> cache = Caffeine.newBuilder()
            .maximumSize(100)
            // expireAfter 的 currentTime 是 System.nanoTime() 是系统启动以来的纳秒数，不是当前时间的毫秒数; 需要转换
            .expireAfter(new Expiry<Class<?>, List<Field>>() {
                @Override
                public long expireAfterCreate(Class<?> key, List<Field> value, long currentTime) {
                    long nowMillis = System.currentTimeMillis();
                    LocalDate today = Instant.ofEpochMilli(nowMillis)
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .toLocalDate();
                    long midnightMillis = today.plusDays(1)
                            .atStartOfDay(ZoneId.of("Asia/Shanghai"))
                            .toInstant()
                            .toEpochMilli();
                    long duration = midnightMillis - nowMillis;
                    return Math.max(0, TimeUnit.MILLISECONDS.toNanos(duration));
                }

                @Override
                public long expireAfterUpdate(Class<?> key, List<Field> value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(Class<?> key, List<Field> value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            //            .ticker(() -> TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()))  // ← 加这一行
            .weakKeys()
            // 在数据不变化的情况下不建议返回 null
            .build(SensitiveDataMasker::resolveSensitiveFields);

    /**
     * 批量处理。
     *
     * @param beans 待导出的 DTO 集合，可为 null
     */
    public void maskBeans(Iterable<?> beans) {
        if (beans == null) {
            return;
        }
        for (Object bean : beans) {
            maskBean(bean);
        }
    }

    /**
     * 处理单个对象。方法直接修改传入对象的敏感字段。
     *
     * @param bean 待处理对象，可为 null
     */
    public void maskBean(Object bean) {
        if (bean == null) {
            return;
        }
        Class<?> clazz = bean.getClass();

        List<Field> fields = cache.get(clazz);
        // ConcurrentMap 为线程安全型，无需同步块
//        List<Field> fields = sensitiveFieldCache.computeIfAbsent(clazz, SensitiveDataMasker::resolveSensitiveFields);
        for (Field field : fields) {
            try {
                String original = (String) field.get(bean);
                if (original != null) {
                    field.set(bean,
                            SensitiveValues.maskIfNeeded(field.getAnnotation(Sensitive.class), original));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "无法访问敏感字段 " + clazz.getSimpleName() + "." + field.getName(), e);
            }
        }
    }

    private static List<Field> resolveSensitiveFields(Class<?> clazz) {
        // 不建议缓存 null 值, null 下次相同的 key 还会执行该方法
        List<Field> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() != String.class) {
                continue;
            }
            if (field.isAnnotationPresent(Sensitive.class)) {
                field.setAccessible(true);
                result.add(field);
            }
        }
        return result.isEmpty() ?
                Collections.emptyList() : result;
    }

}