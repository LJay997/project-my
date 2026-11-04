package com.qq.ijay997.mapper;

import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.support.DemoMockData;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User 与 UserDTO 的转换。
 * <p>
 * 实现类由 MapStruct 在编译期生成，Spring 容器中可直接注入使用。
 *
 * @author ijay997
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserMapper {

    /**
     * 实体转 DTO：同名属性自动映射，phone 由 {@link #fillPhone} 填充。
     *
     * @param user 用户实体
     * @return 用户 DTO
     */
    @Mapping(target = "username", source = "username", qualifiedByName = "trimToNull")
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "createTime", qualifiedByName = "setCreateTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserDTO toDTO(User user);

    /**
     * 创建时间后置处理：若源为 null，则设为当前时间。
     *
     * @param createTime 创建时间
     * @return 处理后的创建时间
     */
    @Named("setCreateTime")
    default LocalDateTime setCreateTime(LocalDateTime createTime) {
        return createTime == null ? LocalDateTime.now() : createTime;
    }

    /**
     * 集合转换：源集合为 null 时返回空列表，避免调用方判空。
     *
     * @param users 用户实体列表
     * @return 用户 DTO 列表
     */
//    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<UserDTO> toDTOList(List<User> users);

    /**
     * DTO 转实体：主键由数据库生成，忽略 DTO 中的 id。
     *
     * @param dto 用户 DTO
     * @return 用户实体
     */
    @Mapping(target = "id", ignore = true)
    User toEntity(UserDTO dto);

    /**
     * 更新已有实体：继承 toEntity 的配置，源为 null 的属性不覆盖目标。
     *
     * @param dto  用户 DTO
     * @param user 待更新的用户实体
     */
    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UserDTO dto, @MappingTarget User user);

    /**
     * 实体转 DTO 后置处理：phone 未落库，按 userId 稳定生成。
     *
     * @param source 用户实体
     * @param target 用户 DTO
     */
    @AfterMapping
    default void fillPhone(User source, @MappingTarget UserDTO target) {
        if (target.getPhone() == null) {
            target.setPhone(DemoMockData.phoneOf(source.getId()));
        }
    }

    /**
     * 用户名去空格，空字符串归一为 null。
     *
     * @param value 原始用户名
     * @return 处理后的用户名
     */
    @Named("trimToNull")
    default String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
