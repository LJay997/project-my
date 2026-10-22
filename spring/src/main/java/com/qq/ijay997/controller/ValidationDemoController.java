package com.qq.ijay997.controller;

import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.ValidatedUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

@Slf4j
@RestController
@RequestMapping("/api/validation")
public class ValidationDemoController {

    @PostMapping("/users")
    public Result<ValidatedUserDTO> createUser(@Valid @RequestBody ValidatedUserDTO user) {
        log.info("创建用户校验通过: {}", user.getUsername());
        return Result.success(user);
    }

    @PostMapping("/users/group-create")
    public Result<ValidatedUserDTO> createUserWithGroup(
            @Validated(ValidatedUserDTO.CreateGroup.class) @RequestBody ValidatedUserDTO user) {
        log.info("分组校验 - 创建用户: {}", user.getUsername());
        return Result.success(user);
    }

    @PutMapping("/users/group-update")
    public Result<ValidatedUserDTO> updateUserWithGroup(
            @Validated(ValidatedUserDTO.UpdateGroup.class) @RequestBody ValidatedUserDTO user) {
        log.info("分组校验 - 更新用户: {}", user.getUsername());
        return Result.success(user);
    }

    @GetMapping("/users/{id}")
    public Result<String> getUserById(@PathVariable @Positive(message = "ID 必须是正整数") Long id) {
        log.info("查询用户 ID: {}", id);
        return Result.success("用户ID: " + id);
    }

    @GetMapping("/query")
    public Result<String> queryWithParams(
            @RequestParam @NotBlank(message = "keyword 不能为空") String keyword,
            @RequestParam @Positive(message = "pageSize 必须是正整数") int pageSize) {
        log.info("查询参数校验通过, keyword: {}, pageSize: {}", keyword, pageSize);
        return Result.success("ok");
    }

    @PostMapping("/users/group-sequence")
    public Result<ValidatedUserDTO> createUserWithSequence(
            @Validated(ValidatedUserDTO.AllSequence.class) @RequestBody ValidatedUserDTO user) {
        log.info("@GroupSequence 顺序校验全部通过: {}", user.getUsername());
        return Result.success(user);
    }
}