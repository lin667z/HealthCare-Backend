package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病人表实体类
 * 对应数据库表：patient
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient")
public class Patient extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登记号 (逻辑主键)
     */
    private String registrationNo;

    /**
     * 性别
     */
    private String gender;

    /**
     * 婚姻状态
     */
    private String maritalStatus;

    /**
     * 受教育程度
     */
    private String educationLevel;
}
