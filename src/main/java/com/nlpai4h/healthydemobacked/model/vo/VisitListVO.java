package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 就诊记录列表项视图对象
 * 用于展示患者所有的就诊（门诊、住院）简要历史列表
 */
@Data
public class VisitListVO {
    /**
     * 就诊记录 ID
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
     * 就诊时年龄
     */
    private String ageAtVisit;
    /**
     * 就诊状态（已完成、进行中等）
     */
    private String visitStatus;
    /**
     * 就诊类型（门诊、住院等）
     */
    private String visitType;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
