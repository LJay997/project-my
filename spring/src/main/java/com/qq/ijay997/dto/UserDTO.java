package com.qq.ijay997.dto;

import com.qq.ijay997.masking.PermissionCodes;
import com.qq.ijay997.masking.Sensitive;
import com.qq.ijay997.masking.SensitiveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;

    @Sensitive(type = SensitiveType.NAME, permission = PermissionCodes.USER_NAME_READ)
    private String username;

    @Sensitive(type = SensitiveType.EMAIL, permission = PermissionCodes.USER_EMAIL_READ)
    private String email;

    @Sensitive(type = SensitiveType.PHONE, permission = PermissionCodes.USER_PHONE_READ)
    private String phone;

    private Integer age;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
