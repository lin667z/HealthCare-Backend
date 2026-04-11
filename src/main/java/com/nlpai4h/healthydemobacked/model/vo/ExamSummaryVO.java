package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

@Data
public class ExamSummaryVO {
    private String examName;
    private String examType;
    private String examDate;
    private String reportDate;
    private String summary;
}
