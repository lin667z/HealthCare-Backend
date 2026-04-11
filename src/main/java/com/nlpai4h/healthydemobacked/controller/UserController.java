package com.nlpai4h.healthydemobacked.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nlpai4h.healthydemobacked.common.annotation.NoControllerLog;
import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.LoginFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.User;
import com.nlpai4h.healthydemobacked.model.vo.UserLoginVO;
import com.nlpai4h.healthydemobacked.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理用户登录、注册及管理员对用户的管理操作
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private IUserService userService;

    /**
     * 用户登录接口
     * 校验账号密码，成功后返回带有Token的用户信息
     *
     * @param loginForm 包含用户名和密码的登录表单
     * @return 包含Token及用户基础信息的登录视图对象
     */
    @PostMapping("/login")
    @NoControllerLog
    public Result<UserLoginVO> login(@RequestBody @Validated LoginFormDTO loginForm) {
        return Result.success(userService.login(loginForm));
    }

    /**
     * 用户注册接口
     * 新增用户并分配默认权限和角色
     *
     * @param user 待注册的用户实体信息
     * @return 注册成功的提示信息
     */
    @PostMapping("/register")
    @NoControllerLog
    public Result<String> register(@RequestBody @Validated User user) {
        userService.register(user);
        return Result.success("注册成功");
    }

    /**
     * 分页获取用户列表 (仅管理员可用)
     * 根据关键字（如用户名）模糊搜索用户
     *
     * @param page     当前页码
     * @param pageSize 每页记录数
     * @param keyword  搜索关键字（可选）
     * @return 包含用户列表的分页结果
     */
    @GetMapping("/page")
    public Result<PageResult> pageQuery(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int pageSize,
                                        @RequestParam(required = false) String keyword) {
        if (!"admin".equals(BaseContext.getCurrentUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        Page<User> userPage = userService.pageQuery(page, pageSize, keyword);
        return Result.success(new PageResult(userPage.getTotal(), userPage.getRecords()));
    }

    /**
     * 修改用户状态 (仅管理员可用)
     * 用于启用或禁用用户账号
     *
     * @param id     目标用户ID
     * @param status 新状态值（如：0-禁用，1-启用）
     * @return 状态更新成功的提示信息
     */
    @PutMapping("/status/{id}/{status}")
    public Result<String> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        if (!"admin".equals(BaseContext.getCurrentUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        userService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 修改用户角色 (仅管理员可用)
     * 更改用户的系统角色（如 user, admin）
     *
     * @param id   目标用户ID
     * @param role 新角色标识
     * @return 角色更新成功的提示信息
     */
    @PutMapping("/role/{id}/{role}")
    public Result<String> updateRole(@PathVariable Long id, @PathVariable String role) {
        if (!"admin".equals(BaseContext.getCurrentUser().getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        userService.updateRole(id, role);
        return Result.success("角色更新成功");
    }
}
