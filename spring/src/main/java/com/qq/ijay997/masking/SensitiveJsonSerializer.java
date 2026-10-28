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

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        Sensitive annotation = property == null ? null : property.getAnnotation(Sensitive.class);
        if (annotation == null && property != null) {
            annotation = property.getContextAnnotation(Sensitive.class);
        }

        SensitiveJsonSerializer contextual = new SensitiveJsonSerializer();
        contextual.sensitive = annotation;
        return contextual;
    }
}
