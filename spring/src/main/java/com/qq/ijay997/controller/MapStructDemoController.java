package com.qq.ijay997.controller;

import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.mapper.UserMapper;
import com.qq.ijay997.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MapStruct 映射效果验证接口。
 *
 * @author ijay997
 */
@RestController
@RequestMapping("/api/mapstruct/users")
public class MapStructDemoController {

    private final UserService userService;
    private final UserMapper userMapper;

    public MapStructDemoController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * 查询全部用户并以 DTO 返回，验证集合映射与 phone 派生。
     *
     * @return 用户 DTO 列表
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> list() {
        return ResponseEntity.ok(userMapper.toDTOList(userService.findAll()));
    }

    /**
     * 按 ID 查询并转换为 DTO。
     *
     * @param id 用户 ID
     * @return 用户 DTO；不存在时返回 404
     */
//    @GetMapping("/{id}")
//    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
//        return userService.findById(id)
//                .map(userMapper::toDTO)
//                .map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.notFound().build());
//    }
}
