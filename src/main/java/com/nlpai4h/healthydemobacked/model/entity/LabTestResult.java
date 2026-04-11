package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检验化验结果表实体类
 * 对应数据库表：lab_test_result
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lab_test_result")
public class LabTestResult extends BaseEntity {

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
     * 检验归一编码(查询)
     */
    private String normalizedCode;

    /**
     * 检验项值
     */
    private String testValue;

    /**
     * 标本名称
     */
    private String specimenName;

    /**
     * 检验查询字段
     */
    private String queryField;

    /**
     * 异常提示
     */
    private String abnormalFlag;

    /**
     * 检验项名称
     */
    private String testName;

    /**
     * 检验项编码（别名）
     */
    private String testCodeAlias;

    /**
     * 正常值范围
     */
    private String referenceRange;

    /**
     * 报告时间
     */
    private String reportTime;

    /**
     * 检验值(格式化)
     */
    private String formattedValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 检验套名称
     */
    private String panelName;

    /**
     * 检验套编码
     */
    private String panelCode;
}
