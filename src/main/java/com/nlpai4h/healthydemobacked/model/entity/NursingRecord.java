package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 护理体征测量表实体类
 * 对应数据库表：nursing_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("nursing_record")
public class NursingRecord extends BaseEntity {

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
     * 测量时间
     */
    private String measureTime;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 上边界
     */
    private String upperLimit;

    /**
     * 下边界
     */
    private String lowerLimit;

    /**
     * 测量值
     */
    private String measureValue;
}
