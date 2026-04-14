package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 护理记录列表项视图对象
 * 用于展示患者生命体征（如体温、脉搏等）的测量历史列表
 */
@Data
public class NursingRecordListVO {
    /**
     * 护理记录 ID
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
     * 测量时间
     */
    private String measureTime;
    /**
     * 测量项名称
     */
    private String itemName;
    /**
     * 正常范围上界
     */
    private String upperLimit;
    /**
     * 正常范围下界
     */
    private String lowerLimit;
    /**
     * 测量数值
     */
    private String measureValue;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
