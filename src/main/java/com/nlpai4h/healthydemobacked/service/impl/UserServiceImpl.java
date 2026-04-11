package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.nlpai4h.healthydemobacked.common.constant.RoleConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.constant.JwtClaimsConstant;
import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.properties.JwtProperties;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.mapper.UserMapper;
import com.nlpai4h.healthydemobacked.model.dto.LoginFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.User;
import com.nlpai4h.healthydemobacked.model.vo.UserLoginVO;
import com.nlpai4h.healthydemobacked.service.IUserService;
import com.nlpai4h.healthydemobacked.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;
    
    private static final String ADMIN_USERNAME = "admin";

    @Override
    public UserLoginVO login(LoginFormDTO loginForm) {
        String username = loginForm.getAccount();
        String password = loginForm.getPassword();
        
        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        // 校验密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 生成Token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.ROLE, user.getRole());

        String token = JwtUtil.createJWT(
                jwtProperties.getMxbSecretKey(),
                jwtProperties.getMxbTtl(),
                claims);

        // 返回VO
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setId(user.getId());
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setName(user.getName());
        userLoginVO.setRole(user.getRole());
        userLoginVO.setToken(token);

        return userLoginVO;
    }

    @Override
    public void register(User user) {
        // 检查用户名是否存在
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .eq(User::getUsername, user.getUsername());
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "用户名已存在");
        }
        
        // 设置默认值
        if (user.getRole() == null) {
            user.setRole(RoleConstant.USER);
        }
        user.setStatus(1);
        
        // 密码加密
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        
        save(user);
    }

    @Override
    public Page<User> pageQuery(int page, int pageSize, String keyword) {
        Page<User> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword));
        }
        
        // 排序：管理员优先，然后按创建时间倒序
        queryWrapper.orderByDesc(User::getRole).orderByDesc(User::getCreateTime);
        
        return page(pageInfo, queryWrapper);
    }

    private void checkAdminOperationPermission(User user, String actionMsg) {
        if (RoleConstant.ADMIN.equals(user.getRole())) {
            BaseContext.UserInfo currentUser = BaseContext.getCurrentUser();
            if (currentUser == null || !ADMIN_USERNAME.equals(currentUser.getUsername())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有默认管理员可以" + actionMsg);
            }
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        checkAdminOperationPermission(user, "操作管理员账户");
        
        // 保护超级管理员
        if (ADMIN_USERNAME.equals(user.getUsername()) && status == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "超级管理员不可被禁用");
        }
        
        user.setStatus(status);
        updateById(user);
    }

    @Override
    public void updateRole(Long id, String role) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        if (RoleConstant.ADMIN.equals(user.getRole()) || RoleConstant.ADMIN.equals(role)) {
            BaseContext.UserInfo currentUser = BaseContext.getCurrentUser();
            if (currentUser == null || !ADMIN_USERNAME.equals(currentUser.getUsername())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有默认管理员可以操作管理员账户或设置管理员");
            }
        }
        
        // 保护超级管理员
        if (ADMIN_USERNAME.equals(user.getUsername()) && !RoleConstant.ADMIN.equals(role)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "超级管理员必须是管理员角色");
        }
        
        user.setRole(role);
        updateById(user);
    }

    @Override
    public boolean removeById(Serializable id) {
        User user = getById(id);
        if (user == null) {
            return false;
        }

        checkAdminOperationPermission(user, "删除管理员账户");

        if (ADMIN_USERNAME.equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "超级管理员不可被删除");
        }
        return super.removeById(id);
    }
}
