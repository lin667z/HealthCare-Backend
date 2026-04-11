package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiagnosisListVO {
    private Long id;
    private String registrationNo;
    private String visitNo;
    private String deptName;
    private String isMain;
    private String status;
    private String diagCode;
    private String icdCode;
    private String diagType;
    private String diagTime;
    private String diagName;
    private LocalDateTime createTime;
}
