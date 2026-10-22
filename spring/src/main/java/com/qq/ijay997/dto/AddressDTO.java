package com.qq.ijay997.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class AddressDTO {

    @NotBlank(message = "省份不能为空")
    @Size(min = 2, max = 20, message = "省份长度必须在 2-20 之间")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(min = 2, max = 20, message = "城市长度必须在 2-20 之间")
    private String city;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 100, message = "详细地址不能超过 100 个字符")
    private String detail;
}
