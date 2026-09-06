package com.qq.ijay997.mapstruct.model.entity;

import com.qq.ijay997.mapstruct.model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户实体（数据库持久层对象）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    private Long id;

    /** 真实姓名 */
    private String name;

    private String email;

    private Gender gender;

    private Integer age;

    private LocalDate birthday;

    private Address address;

    /** 手机号（可能为 null 或空白串，用于演示条件映射） */
    private String phone;

    private LocalDateTime createdAt;
}
