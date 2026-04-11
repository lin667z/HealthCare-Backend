package com.nlpai4h.healthydemobacked.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginFormDTO {
    @NotBlank(message = "账号不能为空")
    private String account;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
