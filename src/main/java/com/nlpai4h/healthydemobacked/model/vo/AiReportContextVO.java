package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiReportContextVO {
    private PatientDetailVO patientSummary;
    private List<DiagnosisSummaryVO> diagnosisSummary;
    private List<ExamSummaryVO> examSummary;
    private List<LabTestSummaryVO> labSummary;
    private List<NursingSummaryVO> nursingSummary;
    private String promptPayload;
}
