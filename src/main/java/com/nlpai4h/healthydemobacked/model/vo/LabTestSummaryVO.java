package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

@Data
public class LabTestSummaryVO {
    private String testName;
    private String panelName;
    private String displayValue;
    private String unit;
    private String referenceRange;
    private String abnormalFlag;
    private String reportTime;
}
