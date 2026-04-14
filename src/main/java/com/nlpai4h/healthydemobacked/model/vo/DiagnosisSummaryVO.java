package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

/**
 * 诊断信息摘要视图对象
 * 用于 AI 报告上下文或简要信息展示
 */
@Data
public class DiagnosisSummaryVO {
    /**
     * 诊断名称
     */
    private String diagName;
    /**
     * 诊断类型
     */
    private String diagType;
    /**
     * 诊断时间
     */
    private String diagTime;
    /**
     * 诊断状态
     */
    private String status;
    /**
     * 是否主要诊断
     */
    private String isMain;
    /**
     * ICD 编码
     */
    private String icdCode;
}
