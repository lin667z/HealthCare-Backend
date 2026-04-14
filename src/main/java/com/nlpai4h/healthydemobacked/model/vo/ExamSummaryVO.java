package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

/**
 * 检查报告摘要视图对象
 * 用于 AI 报告上下文或检查信息概览
 */
@Data
public class ExamSummaryVO {
    /**
     * 检查名称
     */
    private String examName;
    /**
     * 检查类型
     */
    private String examType;
    /**
     * 检查日期
     */
    private String examDate;
    /**
     * 报告日期
     */
    private String reportDate;
    /**
     * 检查所见或结果摘要
     */
    private String summary;
}
