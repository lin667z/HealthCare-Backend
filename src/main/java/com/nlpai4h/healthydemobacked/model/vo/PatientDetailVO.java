package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

/**
 * 患者详细信息视图对象
 * 聚合了患者基本信息及入院、诊断等核心病历摘要信息
 */
@Data
public class PatientDetailVO {
    /** 内部主键ID */
    private Long id;
    /** 挂号单号/就诊登记号 */
    private String registrationNo;
    /** 唯一就诊编号 */
    private String visitNo;
    /** 住院次数 */
    private String hospitalizationCount;
    /** 当前年龄 */
    private String age;
    /** 就诊时年龄 */
    private String ageAtVisit;
    /** 性别 */
    private String gender;
    /** 婚姻状况 */
    private String maritalStatus;
    /** 受教育程度 */
    private String educationLevel;
    /** 就诊科室名称 */
    private String deptName;
    /** 就诊类型（如门诊、住院） */
    private String visitType;
    /** 当前就诊状态 */
    private String visitStatus;
    /** 体格检查：生命体征（血压、心率等） */
    private String physicalExamVitals;
    /** 体格检查：体重 */
    private String physicalExamWeight;
    /** 既往病史 */
    private String pastHistory;
    /** 家族病史 */
    private String familyHistory;
    /** 主诉信息 */
    private String chiefComplaint;
    /** 个人史（如吸烟、饮酒史） */
    private String personalHistory;
    /** 入院时体重 */
    private String admissionWeight;
    /** 出院主要诊断名称 */
    private String dischargeMainDiag;
    /** 出院其他诊断名称 */
    private String dischargeOtherDiag;
    /** 出院主要诊断ICD编码 */
    private String dischargeMainDiagCode;
    /** 出院其他诊断ICD编码 */
    private String dischargeOtherDiagCode;
}
