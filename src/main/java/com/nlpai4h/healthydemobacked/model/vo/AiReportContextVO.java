package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 报告上下文视图对象
 * 封装了生成 AI 报告所需的各种医疗背景数据
 */
@Data
public class AiReportContextVO {
    /**
     * 患者基本信息及就诊摘要
     */
    private PatientDetailVO patientSummary;
    /**
     * 诊断记录摘要列表
     */
    private List<DiagnosisSummaryVO> diagnosisSummary;
    /**
     * 检查报告摘要列表
     */
    private List<ExamSummaryVO> examSummary;
    /**
     * 检验结果摘要列表
     */
    private List<LabTestSummaryVO> labSummary;
    /**
     * 护理记录摘要列表
     */
    private List<NursingSummaryVO> nursingSummary;
    /**
     * 最终发送给 AI 的 Prompt 负载内容
     */
    private String promptPayload;
}
