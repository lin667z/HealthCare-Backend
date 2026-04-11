package com.nlpai4h.healthydemobacked.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.constant.ApplicationStatusConstant;
import com.nlpai4h.healthydemobacked.common.constant.RoleConstant;
import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.mapper.PermissionApplicationMapper;
import com.nlpai4h.healthydemobacked.mapper.PermissionMapper;
import com.nlpai4h.healthydemobacked.mapper.UserMapper;
import com.nlpai4h.healthydemobacked.mapper.UserPermissionMapper;
import com.nlpai4h.healthydemobacked.model.entity.Permission;
import com.nlpai4h.healthydemobacked.model.entity.PermissionApplication;
import com.nlpai4h.healthydemobacked.model.entity.User;
import com.nlpai4h.healthydemobacked.model.entity.UserPermission;
import com.nlpai4h.healthydemobacked.model.vo.PermissionApplicationVO;
import com.nlpai4h.healthydemobacked.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    private final PermissionMapper permissionMapper;
    private final UserPermissionMapper userPermissionMapper;
    private final PermissionApplicationMapper permissionApplicationMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyPermission(Long userId, Long permissionId, String reason) {
        // 1. 检查是否已经拥有该权限
        LambdaQueryWrapper<UserPermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPermission::getUserId, userId)
                .eq(UserPermission::getPermissionId, permissionId);
        if (userPermissionMapper.exists(queryWrapper)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已拥有该权限，无需申请");
        }

        // 2. 检查是否有待审核的申请
        LambdaQueryWrapper<PermissionApplication> appQuery = new LambdaQueryWrapper<>();
        appQuery.eq(PermissionApplication::getUserId, userId)
                .eq(PermissionApplication::getPermissionId, permissionId)
                .eq(PermissionApplication::getStatus, ApplicationStatusConstant.PENDING);
        if (permissionApplicationMapper.exists(appQuery)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已提交过申请，请耐心等待审核");
        }

        // 3. 创建申请
        PermissionApplication application = new PermissionApplication();
        application.setUserId(userId);
        application.setPermissionId(permissionId);
        application.setReason(reason);
        application.setStatus(ApplicationStatusConstant.PENDING);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        
        permissionApplicationMapper.insert(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPermission(Long applicationId, Boolean approved, String auditReason) {
        PermissionApplication application = permissionApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "申请不存在");
        }
        if (!ApplicationStatusConstant.PENDING.equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该申请已被处理");
        }

        if (approved) {
            application.setStatus(ApplicationStatusConstant.APPROVED);
            // 授予权限
            UserPermission userPermission = new UserPermission();
            userPermission.setUserId(application.getUserId());
            userPermission.setPermissionId(application.getPermissionId());
            userPermission.setCreateTime(LocalDateTime.now());
            
            // 检查是否重复插入（防止并发问题）
            LambdaQueryWrapper<UserPermission> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserPermission::getUserId, application.getUserId())
                    .eq(UserPermission::getPermissionId, application.getPermissionId());
            if (!userPermissionMapper.exists(queryWrapper)) {
                userPermissionMapper.insert(userPermission);
            }
            
            // 如果申请的是 role:admin 权限，则直接将用户角色提升为 admin
            Permission permission = permissionMapper.selectById(application.getPermissionId());
            if (permission != null && RoleConstant.ROLE_ADMIN_CODE.equals(permission.getCode())) {
                // 只有默认管理员可以审批管理员权限
                BaseContext.UserInfo currentUser = BaseContext.getCurrentUser();
                if (currentUser == null || !RoleConstant.ADMIN.equals(currentUser.getUsername())) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有默认管理员可以审批管理员权限");
                }
                
                User user = userMapper.selectById(application.getUserId());
                if (user != null) {
                    user.setRole(RoleConstant.ADMIN);
                    userMapper.updateById(user);
                }
            }
        } else {
            application.setStatus(ApplicationStatusConstant.REJECTED);
        }

        application.setAuditReason(auditReason);
        application.setUpdateTime(LocalDateTime.now());
        permissionApplicationMapper.updateById(application);
    }

    @Override
    public List<Permission> getUserPermissions(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && RoleConstant.ADMIN.equals(user.getRole())) {
            // 如果是管理员，返回所有权限
            return permissionMapper.selectList(null);
        }

        // 1. 获取用户关联的权限ID列表
        LambdaQueryWrapper<UserPermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserPermission::getUserId, userId);
        List<UserPermission> userPermissions = userPermissionMapper.selectList(queryWrapper);
        
        if (userPermissions.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> permissionIds = userPermissions.stream()
                .map(UserPermission::getPermissionId)
                .collect(Collectors.toSet());

        // 2. 查询权限详情
        return permissionMapper.selectBatchIds(permissionIds);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return list();
    }

    @Override
    public List<PermissionApplicationVO> getPendingApplications() {
        LambdaQueryWrapper<PermissionApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PermissionApplication::getStatus, ApplicationStatusConstant.PENDING)
                .orderByDesc(PermissionApplication::getCreateTime);
        List<PermissionApplication> applications = permissionApplicationMapper.selectList(queryWrapper);
        
        return convertToVO(applications);
    }

    @Override
    public List<PermissionApplicationVO> getUserApplications(Long userId) {
        LambdaQueryWrapper<PermissionApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PermissionApplication::getUserId, userId)
                .orderByDesc(PermissionApplication::getCreateTime);
        List<PermissionApplication> applications = permissionApplicationMapper.selectList(queryWrapper);
        
        return convertToVO(applications);
    }

    private List<PermissionApplicationVO> convertToVO(List<PermissionApplication> applications) {
        if (applications.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有涉及的用户ID和权限ID
        Set<Long> userIds = applications.stream().map(PermissionApplication::getUserId).collect(Collectors.toSet());
        Set<Long> permissionIds = applications.stream().map(PermissionApplication::getPermissionId).collect(Collectors.toSet());

        // 批量查询用户和权限信息
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Permission> permissionMap = permissionMapper.selectBatchIds(permissionIds).stream()
                .collect(Collectors.toMap(Permission::getId, p -> p));

        return applications.stream().map(app -> {
            PermissionApplicationVO vo = new PermissionApplicationVO();
            BeanUtils.copyProperties(app, vo);
            
            User user = userMap.get(app.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }
            
            Permission permission = permissionMap.get(app.getPermissionId());
            if (permission != null) {
                vo.setPermissionName(permission.getName());
                vo.setPermissionCode(permission.getCode());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }
}
