package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

/**
 * 检验结果摘要视图对象
 * 用于 AI 报告上下文或关键化验指标展示
 */
@Data
public class LabTestSummaryVO {
    /**
     * 检验项名称
     */
    private String testName;
    /**
     * 检验套名称
     */
    private String panelName;
    /**
     * 展示数值
     */
    private String displayValue;
    /**
     * 单位
     */
    private String unit;
    /**
     * 参考范围
     */
    private String referenceRange;
    /**
     * 异常标识
     */
    private String abnormalFlag;
    /**
     * 报告时间
     */
    private String reportTime;
}
