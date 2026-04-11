package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

@Data
public class NursingSummaryVO {
    private String itemName;
    private String latestValue;
    private String latestMeasureTime;
    private String highestValue;
    private String lowestValue;
    private Integer recordCount;
}
