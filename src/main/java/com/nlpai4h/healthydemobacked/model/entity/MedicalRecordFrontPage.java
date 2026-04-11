package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病案首页信息表实体类
 * 对应数据库表：medical_record_front_page
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_record_front_page")
public class MedicalRecordFrontPage extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 首页第几次住院
     */
    private String hospitalizationCount;

    /**
     * 首页就诊时年龄
     */
    private String age;

    /**
     * 首页性别
     */
    private String gender;

    /**
     * 首页出院其他诊断
     */
    private String dischargeOtherDiag;

    /**
     * 首页出院主要诊断
     */
    private String dischargeMainDiag;

    /**
     * 首页出院其他诊断代码
     */
    private String dischargeOtherDiagCode;

    /**
     * 首页出院主要诊断代码
     */
    private String dischargeMainDiagCode;

    /**
     * 首页婚姻状况
     */
    private String maritalStatus;

    /**
     * 首页入院体重
     */
    private String admissionWeight;
}
