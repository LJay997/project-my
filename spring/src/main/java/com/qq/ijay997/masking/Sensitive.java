package com.qq.ijay997.masking;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段声明。
 * <p>
 * 同时服务两种输出场景：
 * <ul>
 *   <li>JSON：借助 {@link JacksonAnnotationsInside} 自动套用 {@link SensitiveJsonSerializer}；</li>
 *   <li>文件：由 {@link SensitiveDataMasker} 反射读取并改写字段值。</li>
 * </ul>
 * 当前用户持有 {@link #permission()} 对应的权限码时输出明文，否则按 {@link #type()} 脱敏。
 *
 * @author ijay997
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive {

    /**
     * 脱敏规则。
     */
    SensitiveType type();

    /**
     * 允许查看明文的权限码，例如 {@code user:email:read}。
     */
    String permission();
}
