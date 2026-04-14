package com.nlpai4h.healthydemobacked.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AI报告实体类
 * 对应数据库表：ai_report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report")
public class AiReport extends BaseEntity {
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
     * 报告内容
     */
    private String report;

    /**
     * 部分报告内容（流式输出时使用）
     */
    private String partialReport;

    /**
     * 状态 0-未生成, 1-已生成, 2-生成中, 3-生成失败
     */
    private Integer status;

    /**
     * 状态信息（如错误信息）
     */
    private String statusMessage;
}
