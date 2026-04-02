package com.qq.ijay997.service;

import com.qq.ijay997.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 
 * @author ijay997
 */
public interface UserService {

    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 根据 ID 查找用户
     * 
     * @param id 用户 ID
     * @return 用户对象
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 创建用户
     * 
     * @param user 用户对象
     * @return 创建后的用户
     */
    User save(User user);

    /**
     * 更新用户
     * 
     * @param id   用户 ID
     * @param user 用户信息
     * @return 更新后的用户
     */
    User update(Long id, User user);

    /**
     * 删除用户
     * 
     * @param id 用户 ID
     */
    void deleteById(Long id);

    /**
     * 根据年龄范围查找用户
     * 
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 用户列表
     */
    List<User> findByAgeRange(Integer minAge, Integer maxAge);
}
