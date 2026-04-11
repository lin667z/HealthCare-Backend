package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.model.dto.LoginFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.User;
import com.nlpai4h.healthydemobacked.model.vo.UserLoginVO;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户服务接口
 */
public interface IUserService extends IService<User> {
    /**
     * 用户登录
     */
    UserLoginVO login(LoginFormDTO loginForm);

    /**
     * 用户注册
     */
    void register(User user);

    /**
     * 分页查询用户
     */
    Page<User> pageQuery(int page, int pageSize, String keyword);

    /**
     * 更新用户状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 更新用户角色
     */
    void updateRole(Long id, String role);
}
