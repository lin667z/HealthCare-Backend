package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LabTestResultListVO {
    private Long id;
    private String registrationNo;
    private String visitNo;
    private String normalizedCode;
    private String testValue;
    private String specimenName;
    private String abnormalFlag;
    private String testName;
    private String referenceRange;
    private String reportTime;
    private String unit;
    private String panelName;
    private LocalDateTime createTime;
}
