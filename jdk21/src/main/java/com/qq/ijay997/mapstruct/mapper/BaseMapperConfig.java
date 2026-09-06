package com.qq.ijay997.mapstruct.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 全局共享映射配置。
 * <p>
 * 各个 {@code @Mapper} 通过 {@code @Mapper(config = BaseMapperConfig.class)} 引用，
 * 用于统一：未映射目标属性的报告策略、集合/Map 的 null 处理策略、组件模型等。
 * <p>
 * 单个 Mapper 上声明的同项配置会覆盖此处的默认值。
 */
@MapperConfig(
        // 目标属性在源中找不到匹配时：仅告警不阻断构建（生产可按团队规范收紧为 ERROR）
        unmappedTargetPolicy = ReportingPolicy.WARN,

        // 源集合/Map 为 null 时，目标返回空集合而不是 null（避免 NPE 与空指针判空样板）
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
        nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface BaseMapperConfig {
}
