package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 临床诊断表实体类
 * 对应数据库表：diagnosis
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diagnosis")
public class Diagnosis extends BaseEntity {

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
     * 诊断科室
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
     * ICD编码
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
}
