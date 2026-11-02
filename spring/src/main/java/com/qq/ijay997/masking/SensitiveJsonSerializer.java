package com.qq.ijay997.masking;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * 敏感字段 JSON 序列化器。
 * <p>
 * 在序列化瞬间读取 {@link Sensitive} 注解与 ThreadLocal 中的权限：
 * 有对应权限输出原文，否则输出 {@link MaskRules} 脱敏后的值。
 * 因为作用于属性级序列化，嵌套对象、集合元素中的敏感字段同样生效。
 *
 * @author ijay997
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private Sensitive sensitive;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(SensitiveValues.maskIfNeeded(sensitive, value));
    }

    /**
     * Jackson 上下文序列化器钩子。
     * <p>
     * 当 Jackson 确定某个属性需要使用本序列化器时，会回调此方法创建一个
     * 携带该属性 @Sensitive 注解信息的上下文化实例，避免在 serialize 中
     * 重复反射读取注解。
     * <p>
     * 注解查找顺序：属性声明本身 → 上下文（如 @JsonSerialize 内联注解）。
     * 若两者都未找到，annotation 为 null，序列化时将直接输出原文。
     *
     * @param prov    序列化提供者，可用于获取其他序列化器或配置
     * @param property 当前被序列化的 Bean 属性，可能为 null（如 Map 动态值）
     * @return 绑定了 @Sensitive 注解的上下文化序列化器实例
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        // 1. 优先从属性声明上直接获取 @Sensitive 注解
        Sensitive annotation = property == null ? null : property.getAnnotation(Sensitive.class);
        // 2. 声明上没有时，回退查找上下文注解（如 @JsonSerialize(using=SensitiveJsonSerializer.class) value 上的注解）
        if (annotation == null && property != null) {
            annotation = property.getContextAnnotation(Sensitive.class);
        }

        // 3. 为当前属性创建专属的序列化器实例，绑定注解信息，避免序列化时重复反射
        SensitiveJsonSerializer contextual = new SensitiveJsonSerializer();
        contextual.sensitive = annotation;
        return contextual;
    }
}