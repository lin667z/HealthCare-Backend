package com.nlpai4h.healthydemobacked.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录表单数据传输对象
 * 用于用户登录时接收账号和密码
 */
@Data
public class LoginFormDTO {
    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    private String account;
    
    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
