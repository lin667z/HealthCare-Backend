package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限申请实体
 */
@Data
@TableName("sys_permission_application")
public class PermissionApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人ID
     */
    private Long userId;

    /**
     * 申请的权限ID
     */
    private Long permissionId;

    /**
     * 申请理由
     */
    private String reason;

    /**
     * 状态：PENDING-待审核, APPROVED-已通过, REJECTED-已拒绝
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

    /**
     * 是否删除
     */
    private Integer isDeleted;
}
