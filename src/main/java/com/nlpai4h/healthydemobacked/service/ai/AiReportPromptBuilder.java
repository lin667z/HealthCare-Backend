package com.nlpai4h.healthydemobacked.service.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nlpai4h.healthydemobacked.common.properties.AiProperties;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.vo.AiReportContextVO;
import com.nlpai4h.healthydemobacked.model.vo.DiagnosisSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.ExamSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.LabTestSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.NursingSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.PatientDetailVO;
import com.nlpai4h.healthydemobacked.service.helper.PatientRecordSummaryHelper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class AiReportPromptBuilder {

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a senior clinical documentation assistant.
            Use only the supplied patient context.
            Do not guess missing facts.
            Do not mention being an AI.
            Do not output patient name, ID number, phone number, address, or contact details.
            If the evidence is insufficient, explicitly write "不足以判断".
            """;

    private static final String DEFAULT_DEVELOPER_PROMPT = """
            Produce a Chinese markdown report only.
            The report must follow this exact structure and keep every heading:
            # AI临床报告
            ## 1. 患者概览
            ## 2. 核心临床判断
            ## 3. 异常检查/检验解读
            ## 4. 风险提示
            ## 5. 诊疗建议
            ## 6. 依据与证据摘要
            ## 7. 局限性声明

            Rules:
            1. Every section must be present.
            2. If data is missing, write "未提供相关数据".
            3. Every recommendation must be tied to evidence.
            4. Evidence should be cited inline with source and time.
            5. Do not output JSON, code fences, greetings, or meta commentary.
            6. Highlight the most important clinical conclusions with markdown bold.
            """;

    @Resource
    private PatientRecordSummaryHelper patientRecordSummaryHelper;
    @Resource
    private AiProperties aiProperties;

    public AiReportContextVO buildContext(QueryFormDTO queryFormDTO) {
        return patientRecordSummaryHelper.buildAiReportContext(queryFormDTO);
    }

    public String buildPrompt(AiReportContextVO contextVO) {
        StringBuilder builder = new StringBuilder();
        builder.append("### System Instructions\n");
        builder.append(resolveSystemPrompt()).append("\n\n");
        builder.append("### Developer Template\n");
        builder.append(resolveDeveloperPrompt()).append("\n\n");
        builder.append("### User Context\n");
        builder.append(buildContextPayload(contextVO));
        return builder.toString();
    }

    private String resolveSystemPrompt() {
        return StrUtil.blankToDefault(aiProperties.getSystemPrompt(), DEFAULT_SYSTEM_PROMPT).trim();
    }

    private String resolveDeveloperPrompt() {
        String configured = StrUtil.blankToDefault(aiProperties.getDeveloperPrompt(), aiProperties.getPrompt());
        return StrUtil.blankToDefault(configured, DEFAULT_DEVELOPER_PROMPT).trim();
    }

    private String buildContextPayload(AiReportContextVO contextVO) {
        StringBuilder sb = new StringBuilder();
        PatientDetailVO patient = contextVO.getPatientSummary();

        sb.append("PatientOverview\n");
        appendField(sb, "registrationNo", patient == null ? null : patient.getRegistrationNo());
        appendField(sb, "visitNo", patient == null ? null : patient.getVisitNo());
        appendField(sb, "gender", patient == null ? null : patient.getGender());
        appendField(sb, "age", patient == null ? null : StrUtil.blankToDefault(patient.getAgeAtVisit(), patient.getAge()));
        appendField(sb, "deptName", patient == null ? null : patient.getDeptName());
        appendField(sb, "visitType", patient == null ? null : patient.getVisitType());
        appendField(sb, "chiefComplaint", patient == null ? null : patient.getChiefComplaint());
        appendField(sb, "pastHistory", patient == null ? null : patient.getPastHistory());
        appendField(sb, "familyHistory", patient == null ? null : patient.getFamilyHistory());
        appendField(sb, "personalHistory", patient == null ? null : patient.getPersonalHistory());
        appendField(sb, "physicalExamVitals", patient == null ? null : patient.getPhysicalExamVitals());
        appendField(sb, "dischargeMainDiag", patient == null ? null : patient.getDischargeMainDiag());
        sb.append('\n');

        sb.append("DiagnosisEvidence\n");
        if (CollUtil.isEmpty(contextVO.getDiagnosisSummary())) {
            sb.append("- none\n");
        } else {
            for (DiagnosisSummaryVO item : contextVO.getDiagnosisSummary()) {
                sb.append("- source=diagnosis")
                        .append(" | name=").append(StrUtil.blankToDefault(item.getDiagName(), "-"))
                        .append(" | type=").append(StrUtil.blankToDefault(item.getDiagType(), "-"))
                        .append(" | primary=").append(StrUtil.blankToDefault(item.getIsMain(), "-"))
                        .append(" | time=").append(StrUtil.blankToDefault(item.getDiagTime(), "-"))
                        .append('\n');
            }
        }
        sb.append('\n');

        sb.append("ExamEvidence\n");
        if (CollUtil.isEmpty(contextVO.getExamSummary())) {
            sb.append("- none\n");
        } else {
            for (ExamSummaryVO item : contextVO.getExamSummary()) {
                sb.append("- source=exam")
                        .append(" | name=").append(StrUtil.blankToDefault(item.getExamName(), "-"))
                        .append(" | type=").append(StrUtil.blankToDefault(item.getExamType(), "-"))
                        .append(" | time=").append(StrUtil.blankToDefault(item.getReportDate(), StrUtil.blankToDefault(item.getExamDate(), "-")))
                        .append(" | summary=").append(StrUtil.blankToDefault(item.getSummary(), "-"))
                        .append('\n');
            }
        }
        sb.append('\n');

        sb.append("LabEvidence\n");
        if (CollUtil.isEmpty(contextVO.getLabSummary())) {
            sb.append("- none\n");
        } else {
            for (LabTestSummaryVO item : contextVO.getLabSummary()) {
                sb.append("- source=lab")
                        .append(" | panel=").append(StrUtil.blankToDefault(item.getPanelName(), "-"))
                        .append(" | test=").append(StrUtil.blankToDefault(item.getTestName(), "-"))
                        .append(" | value=").append(StrUtil.blankToDefault(item.getDisplayValue(), "-"))
                        .append(StrUtil.blankToDefault(item.getUnit(), ""))
                        .append(" | range=").append(StrUtil.blankToDefault(item.getReferenceRange(), "-"))
                        .append(" | abnormal=").append(StrUtil.blankToDefault(item.getAbnormalFlag(), "-"))
                        .append(" | time=").append(StrUtil.blankToDefault(item.getReportTime(), "-"))
                        .append('\n');
            }
        }
        sb.append('\n');

        sb.append("NursingEvidence\n");
        if (CollUtil.isEmpty(contextVO.getNursingSummary())) {
            sb.append("- none\n");
        } else {
            for (NursingSummaryVO item : contextVO.getNursingSummary()) {
                sb.append("- source=nursing")
                        .append(" | item=").append(StrUtil.blankToDefault(item.getItemName(), "-"))
                        .append(" | latest=").append(StrUtil.blankToDefault(item.getLatestValue(), "-"))
                        .append(" | high=").append(StrUtil.blankToDefault(item.getHighestValue(), "-"))
                        .append(" | low=").append(StrUtil.blankToDefault(item.getLowestValue(), "-"))
                        .append(" | time=").append(StrUtil.blankToDefault(item.getLatestMeasureTime(), "-"))
                        .append('\n');
            }
        }
        return sb.toString().trim();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        sb.append("- ").append(label).append(": ").append(value).append('\n');
    }
}
