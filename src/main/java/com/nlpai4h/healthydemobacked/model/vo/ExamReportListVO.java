package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查报告列表项视图对象
 * 用于展示患者所有的检查（如 CT、MRI、超声等）简要列表
 */
@Data
public class ExamReportListVO {
    /**
     * 检查记录 ID
     */
    private Long id;
    /**
     * 登记号
     */
    private String registrationNo;
    /**
     * 就诊号
     */
    private String visitNo;
    /**
     * 检查类型
     */
    private String examType;
    /**
     * 检查名称
     */
    private String examName;
    /**
     * 检查编码
     */
    private String examCode;
    /**
     * 报告日期
     */
    private String reportDate;
    /**
     * 检查日期
     */
    private String examDate;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
