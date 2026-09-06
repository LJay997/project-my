package com.qq.ijay997.mapstruct;

import com.qq.ijay997.mapstruct.mapper.CustomerMapper;
import com.qq.ijay997.mapstruct.model.dto.AddressDTO;
import com.qq.ijay997.mapstruct.model.dto.CustomerDTO;
import com.qq.ijay997.mapstruct.model.entity.Address;
import com.qq.ijay997.mapstruct.model.entity.Customer;
import com.qq.ijay997.mapstruct.model.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CustomerMapper 测试：字段改名、内置类型转换、自定义方法、
 * 条件映射（@Condition）、逆映射、@MappingTarget 更新。
 */
@DisplayName("客户映射测试")
class CustomerMapperTest {

    private final CustomerMapper mapper = CustomerMapper.INSTANCE;

    @Test
    @DisplayName("基础映射：改名 / 枚举转字符串 / 日期格式化 / 嵌套 record / 自定义脱敏与年龄段")
    void shouldMapCustomerToDTO() {
        // given
        Customer customer = Customer.builder()
                .id(1001L)
                .name("张三")
                .email("zhangsan@example.com")
                .gender(Gender.MALE)
                .age(30)
                .birthday(LocalDate.of(1995, 6, 15))
                .address(new Address("广东省", "深圳市", "南山区科技园路1号"))
                .phone("13812345678")
                .build();

        // when
        CustomerDTO dto = mapper.toDTO(customer);

        // then
        assertAll("CustomerDTO 各字段",
                () -> assertEquals(1001L, dto.getId()),
                () -> assertEquals("张三", dto.getCustomerName()),
                () -> assertEquals("zhangsan@example.com", dto.getEmail()),
                () -> assertEquals("MALE", dto.getGender()),
                () -> assertEquals("1995-06-15", dto.getBirthday()),
                () -> assertEquals("成年", dto.getAgeGroup()),
                () -> assertEquals("138****5678", dto.getMaskedPhone())
        );

        // 嵌套对象映射到 record DTO
        AddressDTO address = dto.getAddress();
        assertNotNull(address);
        assertAll("AddressDTO record 组件",
                () -> assertEquals("广东省", address.province()),
                () -> assertEquals("深圳市", address.city()),
                () -> assertEquals("南山区科技园路1号", address.detail())
        );
    }

    @Test
    @DisplayName("条件映射：手机号为空白串时不映射，maskedPhone 为 null")
    void shouldSkipMaskedPhoneWhenPhoneBlank() {
        Customer customer = Customer.builder()
                .id(1002L)
                .name("李四")
                .gender(Gender.FEMALE)
                .age(17)
                .phone("   ")
                .build();

        CustomerDTO dto = mapper.toDTO(customer);

        assertNull(dto.getMaskedPhone(), "空白手机号应被 @Condition 拦截");
        assertEquals("青少年", dto.getAgeGroup(), "17 岁应为青少年");
    }

    @Test
    @DisplayName("自定义转换：null 年龄返回“未知”，老年年龄段")
    void shouldMapAgeGroupForNullAndSenior() {
        Customer unknown = Customer.builder().name("无名").age(null).build();
        Customer senior = Customer.builder().name("老王").age(70).build();

        assertEquals("未知", mapper.toDTO(unknown).getAgeGroup());
        assertEquals("老年", mapper.toDTO(senior).getAgeGroup());
    }

    @Test
    @DisplayName("逆映射：DTO -> 实体，字符串转枚举/日期，忽略字段保持 null")
    void shouldMapDTOToEntity() {
        CustomerDTO dto = CustomerDTO.builder()
                .id(2001L)
                .customerName("王五")
                .email("wangwu@example.com")
                .gender("FEMALE")
                .birthday("1990-03-20")
                .address(new AddressDTO("浙江省", "杭州市", "西湖区文三路"))
                .build();

        Customer entity = mapper.toEntity(dto);

        assertAll(
                () -> assertEquals("王五", entity.getName()),
                () -> assertEquals(Gender.FEMALE, entity.getGender()),
                () -> assertEquals(LocalDate.of(1990, 3, 20), entity.getBirthday()),
                () -> assertEquals("杭州市", entity.getAddress().getCity()),
                () -> assertNull(entity.getAge(), "age 显式 ignore，应为 null"),
                () -> assertNull(entity.getPhone(), "phone 显式 ignore，应为 null"),
                () -> assertNull(entity.getCreatedAt())
        );
    }

    @Test
    @DisplayName("更新既有实体：@MappingTarget 覆盖可变字段，被 ignore 的字段保留原值")
    void shouldUpdateExistingEntity() {
        Customer existing = Customer.builder()
                .id(3001L)
                .name("旧名字")
                .age(28)
                .phone("13900001111")
                .build();

        CustomerDTO patch = CustomerDTO.builder()
                .id(3001L)
                .customerName("新名字")
                .gender("UNKNOWN")
                .build();

        mapper.updateEntity(patch, existing);

        assertAll(
                () -> assertEquals("新名字", existing.getName()),
                () -> assertEquals(Gender.UNKNOWN, existing.getGender()),
                () -> assertEquals(28, existing.getAge(), "ignore 的字段不应被覆盖"),
                () -> assertEquals("13900001111", existing.getPhone(), "ignore 的字段不应被覆盖")
        );
    }
}
