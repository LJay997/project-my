package com.qq.ijay997.masking;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

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
        for (Field field : clazz.getDeclaredFields()) {
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive == null || field.getType() != String.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                String original = (String) field.get(bean);
                if (original != null) {
                    field.set(bean, SensitiveValues.maskIfNeeded(sensitive, original));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(
                        "无法访问敏感字段 " + clazz.getSimpleName() + "." + field.getName(), e);
            }
        }
    }
}
