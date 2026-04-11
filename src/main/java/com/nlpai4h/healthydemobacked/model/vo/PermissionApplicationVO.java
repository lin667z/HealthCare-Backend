package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PermissionApplicationVO {
    private Long id;
    private Long userId;
    private String username;
    private Long permissionId;
    private String permissionName;
    private String permissionCode;
    private String reason;
    private String status;
    private String auditReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
