package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入院记录表实体类
 * 对应数据库表：admission_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admission_record")
public class AdmissionRecord extends BaseEntity {

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
     * 体格检查(生命体征、一般情况)
     */
    private String physicalExamVitals;

    /**
     * 体格检查-体重
     */
    private String physicalExamWeight;

    /**
     * 既往史
     */
    private String pastHistory;

    /**
     * 家族史
     */
    private String familyHistory;

    /**
     * 主诉
     */
    private String chiefComplaint;

    /**
     * 个人史
     */
    private String personalHistory;
}
