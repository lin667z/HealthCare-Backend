package com.nlpai4h.healthydemobacked.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用查询表单数据传输对象
 * 用于在Controller层接收患者相关的查询参数（如就诊记录、AI报告查询等）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryFormDTO {
    /**
     * 就诊号
     */
    private String visitNo;

    /**
     * 登记号
     */
    private String registrationNo;

    /**
     * 是否需要摘要
     */
    private Boolean needSummary;
}
