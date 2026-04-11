package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.entity.Permission;
import com.nlpai4h.healthydemobacked.model.vo.PermissionApplicationVO;
import com.nlpai4h.healthydemobacked.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 权限管理控制器
 * 提供权限的申请、审批、查询等操作接口
 */
@RestController
@RequestMapping("/api/permission")
@Slf4j
@RequiredArgsConstructor
public class PermissionController {

    private final IPermissionService permissionService;

    /**
     * 申请权限
     *
     * @param requestBody 包含 permissionId 和 reason
     * @return 成功
     */
    @PostMapping("/apply")
    public Result<String> applyPermission(@RequestBody Map<String, Object> requestBody) {
        Long permissionId = Long.valueOf(requestBody.get("permissionId").toString());
        String reason = (String) requestBody.get("reason");
        Long userId = BaseContext.getCurrentUser().getUserId();
        
        permissionService.applyPermission(userId, permissionId, reason);
        return Result.success("申请提交成功");
    }

    /**
     * 审批权限申请 (仅管理员)
     *
     * @param requestBody 包含 applicationId, approved, auditReason
     * @return 成功
     */
    @PostMapping("/audit")
    public Result<String> auditPermission(@RequestBody Map<String, Object> requestBody) {
        // 检查管理员权限
        String role = BaseContext.getCurrentUser().getRole();
        if (!"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }

        Long applicationId = Long.valueOf(requestBody.get("applicationId").toString());
        Boolean approved = (Boolean) requestBody.get("approved");
        String auditReason = (String) requestBody.get("auditReason");
        
        permissionService.auditPermission(applicationId, approved, auditReason);
        return Result.success("操作成功");
    }

    /**
     * 获取所有可用权限
     *
     * @return 权限列表
     */
    @GetMapping("/list")
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionService.getAllPermissions());
    }

    /**
     * 获取当前用户的已拥有权限
     *
     * @return 权限列表
     */
    @GetMapping("/my")
    public Result<List<Permission>> getMyPermissions() {
        Long userId = BaseContext.getCurrentUser().getUserId();
        return Result.success(permissionService.getUserPermissions(userId));
    }

    /**
     * 获取待审批列表 (仅管理员)
     *
     * @return 申请列表
     */
    @GetMapping("/applications/pending")
    public Result<List<PermissionApplicationVO>> getPendingApplications() {
        // 检查管理员权限
        String role = BaseContext.getCurrentUser().getRole();
        if (!"admin".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作");
        }
        return Result.success(permissionService.getPendingApplications());
    }

    /**
     * 获取我的申请记录
     *
     * @return 申请列表
     */
    @GetMapping("/applications/my")
    public Result<List<PermissionApplicationVO>> getMyApplications() {
        Long userId = BaseContext.getCurrentUser().getUserId();
        return Result.success(permissionService.getUserApplications(userId));
    }
}
