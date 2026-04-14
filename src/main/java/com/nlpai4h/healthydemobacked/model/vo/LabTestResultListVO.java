package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检验结果列表项视图对象
 * 用于展示患者所有的化验（如血常规、生化等）详细结果列表
 */
@Data
public class LabTestResultListVO {
    /**
     * 检验记录 ID
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
     * 归一化编码
     */
    private String normalizedCode;
    /**
     * 检验数值
     */
    private String testValue;
    /**
     * 标本名称
     */
    private String specimenName;
    /**
     * 异常标识（如：H, L, *）
     */
    private String abnormalFlag;
    /**
     * 检验项名称
     */
    private String testName;
    /**
     * 参考范围
     */
    private String referenceRange;
    /**
     * 报告时间
     */
    private String reportTime;
    /**
     * 单位
     */
    private String unit;
    /**
     * 检验套（面板）名称
     */
    private String panelName;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
