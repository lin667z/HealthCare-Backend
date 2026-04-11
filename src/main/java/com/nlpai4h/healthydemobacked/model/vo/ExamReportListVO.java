package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamReportListVO {
    private Long id;
    private String registrationNo;
    private String visitNo;
    private String examType;
    private String examName;
    private String examCode;
    private String reportDate;
    private String examDate;
    private LocalDateTime createTime;
}
