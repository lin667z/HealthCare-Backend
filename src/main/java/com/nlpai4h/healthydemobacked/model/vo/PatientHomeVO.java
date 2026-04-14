package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

/**
 * 患者首页展示视图对象
 * 用于首页卡片式展示患者的关键信息
 */
@Data
public class PatientHomeVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 登记号
     */
    private String regno;

    /**
     * 就诊号
     */
    private String admno;

    /**
     * 首页第几次住院
     */
    private Integer mrhpHpTimesIn;

    /**
     * 首页就诊时年龄
     */
    private String mrhpHpAge;

    /**
     * 首页性别
     */
    private String mrhpHpGender;

    /**
     * 首页出院主要诊断
     */
    private String mrhpHpDischargedDiag;

    /**
     * 严重状态 (低危/中危/高危)
     */
    private String severityLevel;

    /**
     * 诊断科室
     */
    private String daDiagDept;
}
