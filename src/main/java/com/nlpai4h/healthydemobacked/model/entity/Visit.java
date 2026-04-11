package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病人就诊记录表实体类
 * 对应数据库表：visit
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit")
public class Visit extends BaseEntity {

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
     * 就诊时年龄
     */
    private String ageAtVisit;

    /**
     * 就诊状态
     */
    private String visitStatus;

    /**
     * 就诊类型
     */
    private String visitType;
}
