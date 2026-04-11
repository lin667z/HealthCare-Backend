package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

@Data
public class DiagnosisSummaryVO {
    private String diagName;
    private String diagType;
    private String diagTime;
    private String status;
    private String isMain;
    private String icdCode;
}
