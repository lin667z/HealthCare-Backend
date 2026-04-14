package com.nlpai4h.healthydemobacked.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 报告摘要卡片视图对象
 * 用于展示报告中的关键指标或标签化信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReportSummaryCardVO {
    /**
     * 卡片标签名称（如：风险等级、核心诊断）
     */
    private String label;
    /**
     * 卡片展示数值或内容
     */
    private String value;
    /**
     * 视觉基调（如：success, warning, error, info）
     */
    private String tone;
}
