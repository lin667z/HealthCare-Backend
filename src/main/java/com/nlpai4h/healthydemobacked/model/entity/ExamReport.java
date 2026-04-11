package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检查报告表实体类
 * 对应数据库表：exam_report
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_report")
public class ExamReport extends BaseEntity {

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
     * 检查类型
     */
    private String examType;

    /**
     * 检查结果
     */
    private String examResult;

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
     * 检查所见
     */
    private String examFindings;
}
