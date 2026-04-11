package com.nlpai4h.healthydemobacked.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nlpai4h.healthydemobacked.mapper.UserMapper;
import com.nlpai4h.healthydemobacked.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化全局管理员
 */
@Component
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(String... args) throws Exception {
        String adminUsername = "admin";
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, adminUsername);
        
        if (userMapper.selectCount(queryWrapper) == 0) {
            log.info("初始化全局管理员账号...");
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(BCrypt.hashpw("123456"));
            admin.setName("超级管理员");
            admin.setRole("admin");
            admin.setStatus(1);
            
            userMapper.insert(admin);
            log.info("全局管理员初始化完成，用户名: admin, 默认密码: 123456");
        }
    }
}
