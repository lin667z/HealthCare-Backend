package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录响应视图对象
 * 登录成功后返回给前端的用户基本信息和认证 Token
 */
@Data
public class UserLoginVO implements Serializable {
    /**
     * 用户 ID
     */
    private Long id;
    /**
     * 账号
     */
    private String username;
    /**
     * 姓名
     */
    private String name;
    /**
     * 角色
     */
    private String role;
    /**
     * 访问令牌
     */
    private String token;
}
