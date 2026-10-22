package com.qq.ijay997.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.GroupSequence;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import java.time.LocalDate;
import java.time.Period;

@Data
public class ValidatedUserDTO {

    public interface CreateGroup {}
    public interface UpdateGroup {}

    public interface BasicGroup {}
    public interface BusinessGroup {}

    // 按组的顺序依次执行，前一组没有任何约束报错才进入下一组
    // 每个阶段独立扫描所有声明了 groups = 该阶段组 的注解
    // 所以如果同一个业务规则被错误地同时挂到两个组上（或挂 Default + 某个业务组），就会在多个阶段各跑一遍
    @GroupSequence({Default.class, BasicGroup.class, BusinessGroup.class})
    public interface AllSequence {}

    @NotNull(message = "用户ID不能为空", groups = UpdateGroup.class)
    private Long id;

    @NotBlank(message = "用户名不能为空", groups = BasicGroup.class)
    @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 之间", groups = BasicGroup.class)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线", groups = BasicGroup.class)
    private String username;

    @NotBlank(message = "密码不能为空", groups = CreateGroup.class)
    @Size(min = 8, max = 32, message = "密码长度必须在 8-32 之间", groups = BasicGroup.class)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "密码必须包含大小写字母和数字", groups = BusinessGroup.class)
    private String password;

    @NotBlank(message = "邮箱不能为空", groups = BasicGroup.class)
    @Email(message = "邮箱格式不正确", groups = BasicGroup.class)
    private String email;

    @NotNull(message = "年龄不能为空", groups = BasicGroup.class)
    @Min(value = 0, message = "年龄不能小于 0", groups = BasicGroup.class)
    @Max(value = 150, message = "年龄不能大于 150", groups = BasicGroup.class)
    private Integer age;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确", groups = BusinessGroup.class)
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDate birthday;

    @Valid
    private AddressDTO address;

    @AssertTrue(message = "用户必须年满 18 岁", groups = BusinessGroup.class)
    public boolean isAdult() {
        if (age == null) {
            return true;
        }
        return age >= 18;
    }

    @AssertTrue(message = "出生日期与年龄不匹配", groups = BusinessGroup.class)
    public boolean isBirthdayMatchAge() {
        if (birthday == null || age == null) {
            return true;
        }
        int calculatedAge = Period.between(birthday, LocalDate.now()).getYears();
        return Math.abs(calculatedAge - age) <= 1;
    }
}