package com.qq.ijay997.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;

/**
 * 用户实体类
 * 
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user")
public class User {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 邮箱
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 年龄
     */
    @Column
    private Integer age;
}
