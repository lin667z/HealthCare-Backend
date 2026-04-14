package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

/**
 * 护理记录摘要视图对象
 * 封装了特定测量项（如体温）在一段时间内的统计信息（最新值、最高值、最低值等）
 */
@Data
public class NursingSummaryVO {
    /**
     * 测量项名称
     */
    private String itemName;
    /**
     * 最新测量值
     */
    private String latestValue;
    /**
     * 最新测量时间
     */
    private String latestMeasureTime;
    /**
     * 周期内最高测量值
     */
    private String highestValue;
    /**
     * 周期内最低测量值
     */
    private String lowestValue;
    /**
     * 测量记录总数
     */
    private Integer recordCount;
}
