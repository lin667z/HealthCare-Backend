package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.model.entity.Permission;
import com.nlpai4h.healthydemobacked.model.entity.PermissionApplication;
import com.nlpai4h.healthydemobacked.model.vo.PermissionApplicationVO;

import java.util.List;

public interface IPermissionService extends IService<Permission> {

    /**
     * 申请权限
     *
     * @param userId       用户ID
     * @param permissionId 权限ID
     * @param reason       申请理由
     */
    void applyPermission(Long userId, Long permissionId, String reason);

    /**
     * 审批权限申请
     *
     * @param applicationId 申请ID
     * @param approved      是否通过
     * @param auditReason   审核意见
     */
    void auditPermission(Long applicationId, Boolean approved, String auditReason);

    /**
     * 获取用户的所有权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getUserPermissions(Long userId);

    /**
     * 获取所有可用权限
     *
     * @return 权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 获取所有待审批的申请 (仅管理员)
     *
     * @return 申请列表
     */
    List<PermissionApplicationVO> getPendingApplications();

    /**
     * 获取用户的申请历史
     *
     * @param userId 用户ID
     * @return 申请列表
     */
    List<PermissionApplicationVO> getUserApplications(Long userId);
}
