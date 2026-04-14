package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 权限申请视图对象
 * 封装了申请记录及相关的用户和权限详细信息
 */
@Data
public class PermissionApplicationVO {
    /**
     * 申请记录 ID
     */
    private Long id;
    /**
     * 申请人 ID
     */
    private Long userId;
    /**
     * 申请人账号
     */
    private String username;
    /**
     * 申请权限 ID
     */
    private Long permissionId;
    /**
     * 权限名称
     */
    private String permissionName;
    /**
     * 权限编码
     */
    private String permissionCode;
    /**
     * 申请理由
     */
    private String reason;
    /**
     * 申请状态（PENDING, APPROVED, REJECTED）
     */
    private String status;
    /**
     * 审核意见
     */
    private String auditReason;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
