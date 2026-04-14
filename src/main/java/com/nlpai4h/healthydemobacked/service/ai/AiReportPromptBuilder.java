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

/**
 * AI临床报告提示词构建器
 * 核心职责：组装系统指令、报告格式模板、患者临床上下文数据，生成给AI模型的完整提示词
 * 遵循Spring组件化设计，可直接注入使用
 */
@Component
public class AiReportPromptBuilder {

    /**
     * 默认系统角色提示词
     * 定义AI的身份、行为约束、数据使用规则、隐私保护要求、数据不足时的响应规则
     */
    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a senior clinical documentation assistant.
            Use only the supplied patient context.
            Do not guess missing facts.
            Do not mention being an AI.
            Do not output patient name, ID number, phone number, address, or contact details.
            If the evidence is insufficient, explicitly write "不足以判断".
            """;

    /**
     * 默认开发者格式提示词
     * 定义AI输出报告的语言、格式、固定章节结构、输出约束和排版规则
     */
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

    /**
     * 患者病历摘要助手：用于封装患者临床数据，构建AI报告上下文对象
     */
    @Resource
    private PatientRecordSummaryHelper patientRecordSummaryHelper;

    /**
     * AI配置属性类：用于读取配置文件中自定义的提示词
     */
    @Resource
    private AiProperties aiProperties;

    /**
     * 构建AI报告所需的患者上下文数据
     * @param queryFormDTO 前端查询参数DTO
     * @return AiReportContextVO 封装完整的患者临床上下文视图对象
     */
    public AiReportContextVO buildContext(QueryFormDTO queryFormDTO) {
        return patientRecordSummaryHelper.buildAiReportContext(queryFormDTO);
    }

    /**
     * 核心方法：构建完整的AI提示词
     * 拼接三部分内容：系统指令 + 开发者格式模板 + 患者临床上下文
     * @param contextVO 患者上下文视图对象
     * @return 完整的AI提示词字符串
     */
    public String buildPrompt(AiReportContextVO contextVO) {
        StringBuilder builder = new StringBuilder();
        // 拼接系统指令模块
        builder.append("### System Instructions\n");
        builder.append(resolveSystemPrompt()).append("\n\n");
        // 拼接开发者格式模板模块
        builder.append("### Developer Template\n");
        builder.append(resolveDeveloperPrompt()).append("\n\n");
        // 拼接用户临床上下文模块
        builder.append("### User Context\n");
        builder.append(buildContextPayload(contextVO));
        return builder.toString();
    }

    /**
     * 解析系统提示词
     * 优先级：配置文件systemPrompt > 默认系统提示词
     * @return 最终生效的系统提示词
     */
    private String resolveSystemPrompt() {
        return StrUtil.blankToDefault(aiProperties.getSystemPrompt(), DEFAULT_SYSTEM_PROMPT).trim();
    }

    /**
     * 解析开发者格式提示词
     * 优先级：配置文件developerPrompt > 配置文件prompt > 默认格式提示词
     * @return 最终生效的格式提示词
     */
    private String resolveDeveloperPrompt() {
        String configured = StrUtil.blankToDefault(aiProperties.getDeveloperPrompt(), aiProperties.getPrompt());
        return StrUtil.blankToDefault(configured, DEFAULT_DEVELOPER_PROMPT).trim();
    }

    /**
     * 构建患者临床上下文文本载荷
     * 格式化拼接患者基础信息、诊断、检查、检验、护理五大类临床证据
     * @param contextVO 患者上下文视图对象
     * @return 格式化后的上下文文本
     */
    private String buildContextPayload(AiReportContextVO contextVO) {
        StringBuilder sb = new StringBuilder();
        PatientDetailVO patient = contextVO.getPatientSummary();

        // 1. 患者基础概览信息
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

        // 2. 诊断证据信息
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

        // 3. 检查证据信息（器械/影像检查）
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

        // 4. 检验证据信息（实验室检验）
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

        // 5. 护理证据信息（护理测量数据）
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

    /**
     * 辅助方法：向StringBuilder中追加非空字段
     * 空值字段直接跳过，不拼接
     * @param sb 字符串构建器
     * @param label 字段标签
     * @param value 字段值
     */
    private void appendField(StringBuilder sb, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        sb.append("- ").append(label).append(": ").append(value).append('\n');
    }
}