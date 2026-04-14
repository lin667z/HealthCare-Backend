package com.nlpai4h.healthydemobacked.service.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nlpai4h.healthydemobacked.mapper.VisitMapper;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.Diagnosis;
import com.nlpai4h.healthydemobacked.model.entity.ExamReport;
import com.nlpai4h.healthydemobacked.model.entity.LabTestResult;
import com.nlpai4h.healthydemobacked.model.entity.NursingRecord;
import com.nlpai4h.healthydemobacked.model.entity.Visit;
import com.nlpai4h.healthydemobacked.model.vo.AiReportContextVO;
import com.nlpai4h.healthydemobacked.model.vo.DiagnosisListVO;
import com.nlpai4h.healthydemobacked.model.vo.DiagnosisSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.ExamSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.LabTestSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.NursingSummaryVO;
import com.nlpai4h.healthydemobacked.model.vo.PatientDetailVO;
import com.nlpai4h.healthydemobacked.model.vo.PatientRecordDetailVO;
import com.nlpai4h.healthydemobacked.service.IDiagnosisService;
import com.nlpai4h.healthydemobacked.service.IExamReportService;
import com.nlpai4h.healthydemobacked.service.ILabTestResultService;
import com.nlpai4h.healthydemobacked.service.INursingRecordService;
import com.nlpai4h.healthydemobacked.utils.BeanUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 患者病历摘要构建助手
 * 核心功能：整合患者基础信息、诊断、检查、检验、护理等全维度医疗数据
 * 构建病历详情视图、AI报告上下文、AI提示词载荷
 */
@Component
@Slf4j
public class PatientRecordSummaryHelper {

    /** 诊断信息最大展示条数 */
    private static final int MAX_DIAGNOSIS = 8;
    /** 检查报告最大展示条数 */
    private static final int MAX_EXAMS = 8;
    /** 检验结果最大展示条数 */
    private static final int MAX_LABS = 24;
    /** 护理记录最大展示条数 */
    private static final int MAX_NURSING = 8;

    @Resource
    private PatientDataHelper patientDataHelper;
    @Resource
    private VisitContextHelper visitContextHelper;
    @Resource
    private VisitMapper visitMapper;
    @Resource
    private IDiagnosisService diagnosisService;
    @Resource
    private IExamReportService examReportService;
    @Resource
    private ILabTestResultService labTestResultService;
    @Resource
    private INursingRecordService nursingRecordService;

    /**
     * 构建患者完整病历详情
     * 异步并行查询患者所有维度数据，组装成统一的病历详情视图对象
     *
     * @param queryFormDTO 查询参数（挂号/就诊号）
     * @return 患者病历详情VO
     */
    public PatientRecordDetailVO buildPatientRecordDetail(QueryFormDTO queryFormDTO) {
        // 解析/补全就诊编号
        visitContextHelper.resolveVisitNo(queryFormDTO);
        String registrationNo = queryFormDTO.getRegistrationNo();
        String visitNo = queryFormDTO.getVisitNo();

        // 异步并行查询各类数据，提升接口性能
        CompletableFuture<PatientDetailVO> patientFuture = CompletableFuture.supplyAsync(() -> buildPatientSummary(queryFormDTO));
        CompletableFuture<List<Diagnosis>> diagFuture = CompletableFuture.supplyAsync(() -> listDiagnoses(registrationNo, visitNo));
        CompletableFuture<List<ExamReport>> examFuture = CompletableFuture.supplyAsync(() -> listExamReports(registrationNo, visitNo));
        CompletableFuture<List<LabTestResult>> labFuture = CompletableFuture.supplyAsync(() -> listLabResults(registrationNo, visitNo));
        CompletableFuture<List<NursingRecord>> nursingFuture = CompletableFuture.supplyAsync(() -> listNursingRecords(registrationNo, visitNo));

        // 等待所有异步任务执行完成
        CompletableFuture.allOf(patientFuture, diagFuture, examFuture, labFuture, nursingFuture).join();

        // 获取异步查询结果
        PatientDetailVO patientSummary = patientFuture.join();
        List<Diagnosis> diagnoses = diagFuture.join();
        List<ExamReport> examReports = examFuture.join();
        List<LabTestResult> labResults = labFuture.join();
        List<NursingRecord> nursingRecords = nursingFuture.join();

        // 诊断实体转换为列表VO
        List<DiagnosisListVO> diagnosisDetails = diagnoses.stream().map(this::toDiagnosisListVO).toList();

        // 组装最终病历详情对象
        PatientRecordDetailVO detailVO = new PatientRecordDetailVO();
        detailVO.setPatientSummary(patientSummary);
        detailVO.setPrimaryDiagnosis(selectPrimaryDiagnosis(diagnosisDetails));
        detailVO.setDiagnoses(diagnosisDetails);
        detailVO.setExamReports(examReports);
        detailVO.setLabResults(labResults);
        detailVO.setNursingRecords(nursingRecords);
        detailVO.setExamSummary(buildExamSummary(examReports));
        detailVO.setLabSummary(buildLabSummary(labResults));
        detailVO.setNursingSummary(buildNursingSummary(nursingRecords));
        detailVO.setAiReportContext(buildAiReportContext(detailVO));
        return detailVO;
    }

    /**
     * 根据查询参数构建AI报告上下文
     *
     * @param queryFormDTO 查询参数
     * @return AI报告上下文VO
     */
    public AiReportContextVO buildAiReportContext(QueryFormDTO queryFormDTO) {
        visitContextHelper.resolveVisitNo(queryFormDTO);
        return buildPatientRecordDetail(queryFormDTO).getAiReportContext();
    }

    /**
     * 根据AI上下文构建提示词载荷（用于AI接口调用）
     * 将患者医疗数据格式化为结构化的文本提示词
     *
     * @param contextVO AI报告上下文
     * @return 格式化后的提示词字符串
     */
    public String buildPromptPayload(AiReportContextVO contextVO) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "## Patient Overview");
        PatientDetailVO patient = contextVO.getPatientSummary();
        appendField(sb, "registrationNo", patient.getRegistrationNo());
        appendField(sb, "visitNo", patient.getVisitNo());
        appendField(sb, "gender", patient.getGender());
        appendField(sb, "age", StrUtil.blankToDefault(patient.getAgeAtVisit(), patient.getAge()));
        appendField(sb, "maritalStatus", patient.getMaritalStatus());
        appendField(sb, "educationLevel", patient.getEducationLevel());
        appendField(sb, "visitType", patient.getVisitType());
        appendField(sb, "visitStatus", patient.getVisitStatus());
        appendField(sb, "deptName", patient.getDeptName());
        appendField(sb, "hospitalizationCount", patient.getHospitalizationCount());

        appendLine(sb, "");
        appendLine(sb, "## History And Admission");
        appendField(sb, "chiefComplaint", patient.getChiefComplaint());
        appendField(sb, "pastHistory", patient.getPastHistory());
        appendField(sb, "familyHistory", patient.getFamilyHistory());
        appendField(sb, "personalHistory", patient.getPersonalHistory());
        appendField(sb, "physicalExamVitals", patient.getPhysicalExamVitals());
        appendField(sb, "physicalExamWeight", patient.getPhysicalExamWeight());
        appendField(sb, "admissionWeight", patient.getAdmissionWeight());
        appendField(sb, "dischargeMainDiag", patient.getDischargeMainDiag());
        appendField(sb, "dischargeOtherDiag", patient.getDischargeOtherDiag());

        appendLine(sb, "");
        appendLine(sb, "## Diagnosis Summary");
        appendDiagnosisSummary(sb, contextVO.getDiagnosisSummary());

        appendLine(sb, "");
        appendLine(sb, "## Exam Summary");
        appendExamSummary(sb, contextVO.getExamSummary());

        appendLine(sb, "");
        appendLine(sb, "## Lab Summary");
        appendLabSummary(sb, contextVO.getLabSummary());

        appendLine(sb, "");
        appendLine(sb, "## Nursing Summary");
        appendNursingSummary(sb, contextVO.getNursingSummary());
        return sb.toString();
    }

    /**
     * 构建患者基础信息摘要
     *
     * @param queryFormDTO 查询参数
     * @return 患者基础信息VO
     */
    private PatientDetailVO buildPatientSummary(QueryFormDTO queryFormDTO) {
        String registrationNo = queryFormDTO.getRegistrationNo();
        String visitNo = queryFormDTO.getVisitNo();
        // 调用助手构建基础患者信息
        PatientDetailVO vo = patientDataHelper.buildPatientDetailVO(queryFormDTO);

        // 查询最新就诊记录，补全就诊相关信息
        Visit visit = visitMapper.selectOne(Wrappers.lambdaQuery(Visit.class)
                .eq(Visit::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), Visit::getVisitNo, visitNo)
                .orderByDesc(Visit::getCreateTime)
                .last("LIMIT 1"));
        if (visit != null) {
            vo.setVisitNo(visit.getVisitNo());
            vo.setVisitType(visit.getVisitType());
            vo.setVisitStatus(visit.getVisitStatus());
            vo.setAgeAtVisit(visit.getAgeAtVisit());
        }

        return vo;
    }

    /**
     * 查询患者诊断记录
     *
     * @param registrationNo 挂号号
     * @param visitNo 就诊号
     * @return 诊断记录列表
     */
    private List<Diagnosis> listDiagnoses(String registrationNo, String visitNo) {
        return diagnosisService.list(new LambdaQueryWrapper<Diagnosis>()
                .eq(Diagnosis::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), Diagnosis::getVisitNo, visitNo)
                .orderByDesc(Diagnosis::getCreateTime)
                .orderByDesc(Diagnosis::getId));
    }

    /**
     * 查询患者检查报告
     */
    private List<ExamReport> listExamReports(String registrationNo, String visitNo) {
        return examReportService.list(new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), ExamReport::getVisitNo, visitNo)
                .orderByDesc(ExamReport::getCreateTime)
                .orderByDesc(ExamReport::getId));
    }

    /**
     * 查询患者检验结果
     */
    private List<LabTestResult> listLabResults(String registrationNo, String visitNo) {
        return labTestResultService.list(new LambdaQueryWrapper<LabTestResult>()
                .eq(LabTestResult::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), LabTestResult::getVisitNo, visitNo)
                .orderByDesc(LabTestResult::getCreateTime)
                .orderByDesc(LabTestResult::getId));
    }

    /**
     * 查询患者护理记录
     */
    private List<NursingRecord> listNursingRecords(String registrationNo, String visitNo) {
        return nursingRecordService.list(new LambdaQueryWrapper<NursingRecord>()
                .eq(NursingRecord::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), NursingRecord::getVisitNo, visitNo)
                .orderByDesc(NursingRecord::getCreateTime)
                .orderByDesc(NursingRecord::getId));
    }

    /**
     * 筛选主要诊断
     * 排序规则：主要诊断优先 > 诊断时间倒序
     *
     * @param diagnoses 诊断列表
     * @return 主要诊断VO
     */
    private DiagnosisListVO selectPrimaryDiagnosis(List<DiagnosisListVO> diagnoses) {
        if (CollUtil.isEmpty(diagnoses)) {
            return null;
        }
        return diagnoses.stream()
                .sorted(Comparator
                        .comparing((DiagnosisListVO item) -> !isMainDiagnosis(item.getIsMain()))
                        .thenComparing(item -> StrUtil.emptyToDefault(item.getDiagTime(), ""), Comparator.reverseOrder()))
                .findFirst()
                .orElse(diagnoses.get(0));
    }

    /**
     * 构建诊断摘要（去重+数量限制）
     */
    private List<DiagnosisSummaryVO> buildDiagnosisSummary(List<DiagnosisListVO> diagnoses) {
        if (CollUtil.isEmpty(diagnoses)) {
            return List.of();
        }
        // 有序集合去重
        Set<String> seen = new LinkedHashSet<>();
        List<DiagnosisSummaryVO> results = new ArrayList<>();
        for (DiagnosisListVO diagnosis : diagnoses) {
            String key = StrUtil.blankToDefault(diagnosis.getDiagName(), diagnosis.getIcdCode());
            if (StrUtil.isBlank(key) || !seen.add(key)) {
                continue;
            }
            DiagnosisSummaryVO summaryVO = new DiagnosisSummaryVO();
            summaryVO.setDiagName(diagnosis.getDiagName());
            summaryVO.setDiagType(diagnosis.getDiagType());
            summaryVO.setDiagTime(diagnosis.getDiagTime());
            summaryVO.setStatus(diagnosis.getStatus());
            summaryVO.setIsMain(diagnosis.getIsMain());
            summaryVO.setIcdCode(diagnosis.getIcdCode());
            results.add(summaryVO);
            // 达到最大数量则终止
            if (results.size() >= MAX_DIAGNOSIS) {
                break;
            }
        }
        return results;
    }

    /**
     * 构建检查报告摘要（去重+文本裁剪+数量限制）
     */
    private List<ExamSummaryVO> buildExamSummary(List<ExamReport> examReports) {
        if (CollUtil.isEmpty(examReports)) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<ExamSummaryVO> results = new ArrayList<>();
        for (ExamReport report : examReports) {
            String key = StrUtil.blankToDefault(report.getExamCode(), report.getExamName() + "|" + report.getExamType());
            if (!seen.add(key)) {
                continue;
            }
            ExamSummaryVO summaryVO = new ExamSummaryVO();
            summaryVO.setExamName(report.getExamName());
            summaryVO.setExamType(report.getExamType());
            summaryVO.setExamDate(report.getExamDate());
            summaryVO.setReportDate(report.getReportDate());
            // 拼接检查结果+发现，裁剪长度
            summaryVO.setSummary(trimText(joinWithSeparator("; ", report.getExamResult(), report.getExamFindings()), 280));
            results.add(summaryVO);
            if (results.size() >= MAX_EXAMS) {
                break;
            }
        }
        return results;
    }

    /**
     * 构建检验结果摘要
     * 优先级：异常结果 > 最新结果，去重+数量限制
     */
    private List<LabTestSummaryVO> buildLabSummary(List<LabTestResult> labResults) {
        if (CollUtil.isEmpty(labResults)) {
            return List.of();
        }
        LinkedHashMap<String, LabTestResult> abnormal = new LinkedHashMap<>();
        LinkedHashMap<String, LabTestResult> latest = new LinkedHashMap<>();
        for (LabTestResult result : labResults) {
            String key = normalizeLabKey(result);
            if (StrUtil.isBlank(key)) {
                continue;
            }
            latest.putIfAbsent(key, result);
            // 标记异常结果
            if (isAbnormal(result.getAbnormalFlag())) {
                abnormal.putIfAbsent(key, result);
            }
        }
        List<LabTestSummaryVO> summaries = new ArrayList<>();
        // 优先添加异常检验项
        appendLabEntries(summaries, abnormal.values());
        // 补充最新检验项直至最大数量
        if (summaries.size() < MAX_LABS) {
            appendLabEntries(summaries, latest.values());
        }
        return summaries.stream().limit(MAX_LABS).toList();
    }

    /**
     * 追加检验数据到摘要列表（去重）
     */
    private void appendLabEntries(List<LabTestSummaryVO> target, Iterable<LabTestResult> rows) {
        Set<String> existing = target.stream()
                .map(item -> normalizeLabKey(item.getTestName(), item.getPanelName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (LabTestResult result : rows) {
            String key = normalizeLabKey(result);
            if (!existing.add(key)) {
                continue;
            }
            LabTestSummaryVO summaryVO = new LabTestSummaryVO();
            summaryVO.setTestName(result.getTestName());
            summaryVO.setPanelName(result.getPanelName());
            summaryVO.setDisplayValue(StrUtil.blankToDefault(result.getFormattedValue(), result.getTestValue()));
            summaryVO.setUnit(result.getUnit());
            summaryVO.setReferenceRange(result.getReferenceRange());
            summaryVO.setAbnormalFlag(result.getAbnormalFlag());
            summaryVO.setReportTime(result.getReportTime());
            target.add(summaryVO);
            if (target.size() >= MAX_LABS) {
                break;
            }
        }
    }

    /**
     * 构建护理记录摘要
     * 按项目分组，优先展示生命体征项，取最新值/极值
     */
    private List<NursingSummaryVO> buildNursingSummary(List<NursingRecord> nursingRecords) {
        if (CollUtil.isEmpty(nursingRecords)) {
            return List.of();
        }
        // 按护理项目名称分组
        Map<String, List<NursingRecord>> grouped = nursingRecords.stream()
                .filter(item -> StrUtil.isNotBlank(item.getItemName()))
                .collect(Collectors.groupingBy(NursingRecord::getItemName, LinkedHashMap::new, Collectors.toList()));

        // 排序：优先生命体征项 > 时间倒序
        List<Map.Entry<String, List<NursingRecord>>> preferred = grouped.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, List<NursingRecord>> entry) -> !isPreferredNursingItem(entry.getKey()))
                        .thenComparing(entry -> entry.getValue().get(0).getCreateTime(), Comparator.reverseOrder()))
                .toList();

        List<NursingSummaryVO> results = new ArrayList<>();
        for (Map.Entry<String, List<NursingRecord>> entry : preferred) {
            List<NursingRecord> rows = entry.getValue();
            NursingRecord latest = rows.get(0);
            NursingSummaryVO summaryVO = new NursingSummaryVO();
            summaryVO.setItemName(entry.getKey());
            summaryVO.setLatestValue(latest.getMeasureValue());
            summaryVO.setLatestMeasureTime(latest.getMeasureTime());
            // 计算最高/最低值
            summaryVO.setHighestValue(findExtreme(rows, true));
            summaryVO.setLowestValue(findExtreme(rows, false));
            summaryVO.setRecordCount(rows.size());
            results.add(summaryVO);
            if (results.size() >= MAX_NURSING) {
                break;
            }
        }
        return results;
    }

    /**
     * 根据病历详情构建AI报告上下文
     */
    private AiReportContextVO buildAiReportContext(PatientRecordDetailVO detailVO) {
        AiReportContextVO contextVO = new AiReportContextVO();
        contextVO.setPatientSummary(detailVO.getPatientSummary());
        contextVO.setDiagnosisSummary(buildDiagnosisSummary(detailVO.getDiagnoses()));
        contextVO.setExamSummary(detailVO.getExamSummary());
        contextVO.setLabSummary(detailVO.getLabSummary());
        contextVO.setNursingSummary(detailVO.getNursingSummary());
        // 构建AI提示词
        contextVO.setPromptPayload(buildPromptPayload(contextVO));
        return contextVO;
    }

    /**
     * 诊断实体转换为列表VO
     */
    private DiagnosisListVO toDiagnosisListVO(Diagnosis diagnosis) {
        return BeanUtil.convert(diagnosis, DiagnosisListVO.class);
    }

    /**
     * 文本裁剪工具：去除换行/多余空格，限制最大长度
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 处理后文本
     */
    private String trimText(String text, int maxLength) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        // 替换换行符为空格，合并多余空格
        String clean = StrUtil.replace(text, "\n", " ");
        clean = StrUtil.replace(clean, "\r", " ");
        clean = StrUtil.trim(clean).replaceAll("\\s+", " ");
        if (clean.length() <= maxLength) {
            return clean;
        }
        // 超长裁剪并追加省略号
        return clean.substring(0, maxLength) + "...";
    }

    /**
     * 字符串拼接：追加行
     */
    private void appendLine(StringBuilder sb, String line) {
        sb.append(line).append('\n');
    }

    /**
     * 字符串拼接：追加字段（空值则跳过）
     */
    private void appendField(StringBuilder sb, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        sb.append("- ").append(label).append(": ").append(value).append('\n');
    }

    /**
     * 拼接诊断摘要到提示词
     */
    private void appendDiagnosisSummary(StringBuilder sb, List<DiagnosisSummaryVO> diagnoses) {
        if (CollUtil.isEmpty(diagnoses)) {
            appendLine(sb, "- none");
            return;
        }
        for (DiagnosisSummaryVO item : diagnoses) {
            sb.append("- ")
                    .append(StrUtil.blankToDefault(item.getDiagName(), "unnamed diagnosis"))
                    .append(" | type: ").append(StrUtil.blankToDefault(item.getDiagType(), "-"))
                    .append(" | time: ").append(StrUtil.blankToDefault(item.getDiagTime(), "-"))
                    .append(" | primary: ").append(StrUtil.blankToDefault(item.getIsMain(), "-"))
                    .append(" | status: ").append(StrUtil.blankToDefault(item.getStatus(), "-"))
                    .append('\n');
        }
    }

    /**
     * 拼接检查摘要到提示词
     */
    private void appendExamSummary(StringBuilder sb, List<ExamSummaryVO> exams) {
        if (CollUtil.isEmpty(exams)) {
            appendLine(sb, "- none");
            return;
        }
        for (ExamSummaryVO item : exams) {
            sb.append("- ")
                    .append(StrUtil.blankToDefault(item.getExamName(), "unnamed exam"))
                    .append(" | type: ").append(StrUtil.blankToDefault(item.getExamType(), "-"))
                    .append(" | date: ").append(StrUtil.blankToDefault(item.getReportDate(), StrUtil.blankToDefault(item.getExamDate(), "-")))
                    .append(" | summary: ").append(StrUtil.blankToDefault(item.getSummary(), "-"))
                    .append('\n');
        }
    }

    /**
     * 拼接检验摘要到提示词
     */
    private void appendLabSummary(StringBuilder sb, List<LabTestSummaryVO> labs) {
        if (CollUtil.isEmpty(labs)) {
            appendLine(sb, "- none");
            return;
        }
        for (LabTestSummaryVO item : labs) {
            sb.append("- ")
                    .append(StrUtil.blankToDefault(item.getTestName(), "unnamed lab"))
                    .append(" | value: ").append(StrUtil.blankToDefault(item.getDisplayValue(), "-"))
                    .append(StrUtil.blankToDefault(item.getUnit(), ""))
                    .append(" | range: ").append(StrUtil.blankToDefault(item.getReferenceRange(), "-"))
                    .append(" | abnormal: ").append(StrUtil.blankToDefault(item.getAbnormalFlag(), "no"))
                    .append(" | time: ").append(StrUtil.blankToDefault(item.getReportTime(), "-"))
                    .append('\n');
        }
    }

    /**
     * 拼接护理摘要到提示词
     */
    private void appendNursingSummary(StringBuilder sb, List<NursingSummaryVO> nursing) {
        if (CollUtil.isEmpty(nursing)) {
            appendLine(sb, "- none");
            return;
        }
        for (NursingSummaryVO item : nursing) {
            sb.append("- ")
                    .append(StrUtil.blankToDefault(item.getItemName(), "unnamed item"))
                    .append(" | latest: ").append(StrUtil.blankToDefault(item.getLatestValue(), "-"))
                    .append(" | high: ").append(StrUtil.blankToDefault(item.getHighestValue(), "-"))
                    .append(" | low: ").append(StrUtil.blankToDefault(item.getLowestValue(), "-"))
                    .append(" | time: ").append(StrUtil.blankToDefault(item.getLatestMeasureTime(), "-"))
                    .append('\n');
        }
    }

    /**
     * 判断是否为主要诊断（支持中英文/数字标识）
     */
    private boolean isMainDiagnosis(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.contains("main")
               || normalized.contains("primary")
               || normalized.contains("主")
               || normalized.equals("1")
               || normalized.equals("y")
               || normalized.equals("yes")
               || normalized.equals("true");
    }

    /**
     * 判断检验结果是否异常
     */
    private boolean isAbnormal(String abnormalFlag) {
        return StrUtil.isNotBlank(abnormalFlag)
               && !"0".equals(abnormalFlag.trim())
               && !"normal".equalsIgnoreCase(abnormalFlag.trim())
               && !"正常".equals(abnormalFlag.trim());
    }

    /**
     * 判断是否为优先展示的护理项（生命体征类）
     */
    private boolean isPreferredNursingItem(String itemName) {
        String value = StrUtil.blankToDefault(itemName, "").toLowerCase();
        return Arrays.asList("体温", "脉搏", "呼吸", "血压", "血糖", "血氧", "体重",
                        "temperature", "pulse", "respiration", "blood pressure", "glucose", "oxygen", "weight")
                .stream()
                .anyMatch(value::contains);
    }

    /**
     * 查找护理记录的极值（最高/最低值）
     *
     * @param rows 护理记录列表
     * @param high true-最大值 false-最小值
     * @return 极值展示文本
     */
    private String findExtreme(List<NursingRecord> rows, boolean high) {
        Double candidate = null;
        String display = null;
        for (NursingRecord row : rows) {
            Double value = parseDouble(row.getMeasureValue());
            if (value == null) {
                if (display == null) {
                    display = row.getMeasureValue();
                }
                continue;
            }
            // 比较并更新极值
            if (candidate == null || (high ? value > candidate : value < candidate)) {
                candidate = value;
                display = row.getMeasureValue();
            }
        }
        return display;
    }

    /**
     * 解析字符串为Double数值（过滤非数字字符）
     */
    private Double parseDouble(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        // 仅保留数字、小数点、负号
        String numeric = value.replaceAll("[^0-9.\\-]", "");
        if (StrUtil.isBlank(numeric)) {
            return null;
        }
        try {
            return Double.parseDouble(numeric);
        } catch (NumberFormatException ex) {
            log.debug("Unable to parse numeric value: {}", value);
            return null;
        }
    }

    /**
     * 标准化检验数据唯一Key（用于去重）
     */
    private String normalizeLabKey(LabTestResult result) {
        return normalizeLabKey(StrUtil.blankToDefault(result.getNormalizedCode(), result.getTestName()), result.getPanelName());
    }

    /**
     * 重载：标准化检验Key
     */
    private String normalizeLabKey(String testName, String panelName) {
        return StrUtil.blankToDefault(testName, "") + "|" + StrUtil.blankToDefault(panelName, "");
    }

    /**
     * 拼接多个字符串，过滤空值，使用分隔符连接
     */
    private String joinWithSeparator(String separator, String... values) {
        return Arrays.stream(values)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(separator));
    }
}