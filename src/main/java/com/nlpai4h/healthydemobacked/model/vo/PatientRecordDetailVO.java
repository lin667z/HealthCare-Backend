package com.nlpai4h.healthydemobacked.model.vo;

import com.nlpai4h.healthydemobacked.model.entity.ExamReport;
import com.nlpai4h.healthydemobacked.model.entity.LabTestResult;
import com.nlpai4h.healthydemobacked.model.entity.NursingRecord;
import lombok.Data;

import java.util.List;

/**
 * 患者完整病历详情视图对象
 * 用于向前端返回包含患者基本信息、诊断、检查、检验、护理记录及 AI 报告上下文的完整数据
 */
@Data
public class PatientRecordDetailVO {

    /** 患者基本信息摘要 */
    private PatientDetailVO patientSummary;

    /** 主要诊断信息 */
    private DiagnosisListVO primaryDiagnosis;

    /** 诊断列表（含主要诊断及其他诊断） */
    private List<DiagnosisListVO> diagnoses;

    /** 检查报告原始列表 */
    private List<ExamReport> examReports;

    /** 检验结果原始列表 */
    private List<LabTestResult> labResults;

    /** 护理记录原始列表 */
    private List<NursingRecord> nursingRecords;

    /** 检查报告摘要列表（用于概览） */
    private List<ExamSummaryVO> examSummary;

    /** 检验报告摘要列表（用于概览） */
    private List<LabTestSummaryVO> labSummary;

    /** 护理记录摘要列表（用于概览） */
    private List<NursingSummaryVO> nursingSummary;

    /** AI 报告生成所需上下文信息 */
    private AiReportContextVO aiReportContext;
}
