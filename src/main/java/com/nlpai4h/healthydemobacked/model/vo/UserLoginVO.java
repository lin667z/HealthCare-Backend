package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginVO implements Serializable {
    private Long id;
    private String username;
    private String name;
    private String role;
    private String token;
}
