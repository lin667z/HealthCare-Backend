package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 诊断列表展示视图对象
 * 用于在管理后台或列表中展示诊断记录的详细信息
 */
@Data
public class DiagnosisListVO {
    /**
     * 诊断记录 ID
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
     * 诊断科室名称
     */
    private String deptName;
    /**
     * 是否主要诊断
     */
    private String isMain;
    /**
     * 诊断状态
     */
    private String status;
    /**
     * 诊断编码
     */
    private String diagCode;
    /**
     * ICD 编码
     */
    private String icdCode;
    /**
     * 诊断类型
     */
    private String diagType;
    /**
     * 诊断时间
     */
    private String diagTime;
    /**
     * 诊断名称
     */
    private String diagName;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
