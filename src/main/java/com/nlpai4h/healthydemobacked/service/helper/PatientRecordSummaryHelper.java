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
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
@Slf4j
public class PatientRecordSummaryHelper {

    private static final int MAX_DIAGNOSIS = 8;
    private static final int MAX_EXAMS = 8;
    private static final int MAX_LABS = 24;
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

    public PatientRecordDetailVO buildPatientRecordDetail(QueryFormDTO queryFormDTO) {
        visitContextHelper.resolveVisitNo(queryFormDTO);
        String registrationNo = queryFormDTO.getRegistrationNo();
        String visitNo = queryFormDTO.getVisitNo();

        CompletableFuture<PatientDetailVO> patientFuture = CompletableFuture.supplyAsync(() -> buildPatientSummary(queryFormDTO));
        CompletableFuture<List<Diagnosis>> diagFuture = CompletableFuture.supplyAsync(() -> listDiagnoses(registrationNo, visitNo));
        CompletableFuture<List<ExamReport>> examFuture = CompletableFuture.supplyAsync(() -> listExamReports(registrationNo, visitNo));
        CompletableFuture<List<LabTestResult>> labFuture = CompletableFuture.supplyAsync(() -> listLabResults(registrationNo, visitNo));
        CompletableFuture<List<NursingRecord>> nursingFuture = CompletableFuture.supplyAsync(() -> listNursingRecords(registrationNo, visitNo));

        CompletableFuture.allOf(patientFuture, diagFuture, examFuture, labFuture, nursingFuture).join();

        PatientDetailVO patientSummary = patientFuture.join();
        List<Diagnosis> diagnoses = diagFuture.join();
        List<ExamReport> examReports = examFuture.join();
        List<LabTestResult> labResults = labFuture.join();
        List<NursingRecord> nursingRecords = nursingFuture.join();

        List<DiagnosisListVO> diagnosisDetails = diagnoses.stream().map(this::toDiagnosisListVO).toList();

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

    public AiReportContextVO buildAiReportContext(QueryFormDTO queryFormDTO) {
        visitContextHelper.resolveVisitNo(queryFormDTO);
        return buildPatientRecordDetail(queryFormDTO).getAiReportContext();
    }

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

    private PatientDetailVO buildPatientSummary(QueryFormDTO queryFormDTO) {
        String registrationNo = queryFormDTO.getRegistrationNo();
        String visitNo = queryFormDTO.getVisitNo();
        PatientDetailVO vo = patientDataHelper.buildPatientDetailVO(queryFormDTO);

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

    private List<Diagnosis> listDiagnoses(String registrationNo, String visitNo) {
        return diagnosisService.list(new LambdaQueryWrapper<Diagnosis>()
                .eq(Diagnosis::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), Diagnosis::getVisitNo, visitNo)
                .orderByDesc(Diagnosis::getCreateTime)
                .orderByDesc(Diagnosis::getId));
    }

    private List<ExamReport> listExamReports(String registrationNo, String visitNo) {
        return examReportService.list(new LambdaQueryWrapper<ExamReport>()
                .eq(ExamReport::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), ExamReport::getVisitNo, visitNo)
                .orderByDesc(ExamReport::getCreateTime)
                .orderByDesc(ExamReport::getId));
    }

    private List<LabTestResult> listLabResults(String registrationNo, String visitNo) {
        return labTestResultService.list(new LambdaQueryWrapper<LabTestResult>()
                .eq(LabTestResult::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), LabTestResult::getVisitNo, visitNo)
                .orderByDesc(LabTestResult::getCreateTime)
                .orderByDesc(LabTestResult::getId));
    }

    private List<NursingRecord> listNursingRecords(String registrationNo, String visitNo) {
        return nursingRecordService.list(new LambdaQueryWrapper<NursingRecord>()
                .eq(NursingRecord::getRegistrationNo, registrationNo)
                .eq(StrUtil.isNotBlank(visitNo), NursingRecord::getVisitNo, visitNo)
                .orderByDesc(NursingRecord::getCreateTime)
                .orderByDesc(NursingRecord::getId));
    }

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

    private List<DiagnosisSummaryVO> buildDiagnosisSummary(List<DiagnosisListVO> diagnoses) {
        if (CollUtil.isEmpty(diagnoses)) {
            return List.of();
        }
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
            if (results.size() >= MAX_DIAGNOSIS) {
                break;
            }
        }
        return results;
    }

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
            summaryVO.setSummary(trimText(joinWithSeparator("; ", report.getExamResult(), report.getExamFindings()), 280));
            results.add(summaryVO);
            if (results.size() >= MAX_EXAMS) {
                break;
            }
        }
        return results;
    }

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
            if (isAbnormal(result.getAbnormalFlag())) {
                abnormal.putIfAbsent(key, result);
            }
        }
        List<LabTestSummaryVO> summaries = new ArrayList<>();
        appendLabEntries(summaries, abnormal.values());
        if (summaries.size() < MAX_LABS) {
            appendLabEntries(summaries, latest.values());
        }
        return summaries.stream().limit(MAX_LABS).toList();
    }

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

    private List<NursingSummaryVO> buildNursingSummary(List<NursingRecord> nursingRecords) {
        if (CollUtil.isEmpty(nursingRecords)) {
            return List.of();
        }
        Map<String, List<NursingRecord>> grouped = nursingRecords.stream()
                .filter(item -> StrUtil.isNotBlank(item.getItemName()))
                .collect(Collectors.groupingBy(NursingRecord::getItemName, LinkedHashMap::new, Collectors.toList()));

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

    private AiReportContextVO buildAiReportContext(PatientRecordDetailVO detailVO) {
        AiReportContextVO contextVO = new AiReportContextVO();
        contextVO.setPatientSummary(detailVO.getPatientSummary());
        contextVO.setDiagnosisSummary(buildDiagnosisSummary(detailVO.getDiagnoses()));
        contextVO.setExamSummary(detailVO.getExamSummary());
        contextVO.setLabSummary(detailVO.getLabSummary());
        contextVO.setNursingSummary(detailVO.getNursingSummary());
        contextVO.setPromptPayload(buildPromptPayload(contextVO));
        return contextVO;
    }

    private DiagnosisListVO toDiagnosisListVO(Diagnosis diagnosis) {
        return BeanUtil.convert(diagnosis, DiagnosisListVO.class);
    }

    private String trimText(String text, int maxLength) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String clean = StrUtil.replace(text, "\n", " ");
        clean = StrUtil.replace(clean, "\r", " ");
        clean = StrUtil.trim(clean).replaceAll("\\s+", " ");
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength) + "...";
    }

    private void appendLine(StringBuilder sb, String line) {
        sb.append(line).append('\n');
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        sb.append("- ").append(label).append(": ").append(value).append('\n');
    }

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

    private boolean isAbnormal(String abnormalFlag) {
        return StrUtil.isNotBlank(abnormalFlag)
                && !"0".equals(abnormalFlag.trim())
                && !"normal".equalsIgnoreCase(abnormalFlag.trim())
                && !"正常".equals(abnormalFlag.trim());
    }

    private boolean isPreferredNursingItem(String itemName) {
        String value = StrUtil.blankToDefault(itemName, "").toLowerCase();
        return Arrays.asList("体温", "脉搏", "呼吸", "血压", "血糖", "血氧", "体重",
                        "temperature", "pulse", "respiration", "blood pressure", "glucose", "oxygen", "weight")
                .stream()
                .anyMatch(value::contains);
    }

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
            if (candidate == null || (high ? value > candidate : value < candidate)) {
                candidate = value;
                display = row.getMeasureValue();
            }
        }
        return display;
    }

    private Double parseDouble(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
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

    private String normalizeLabKey(LabTestResult result) {
        return normalizeLabKey(StrUtil.blankToDefault(result.getNormalizedCode(), result.getTestName()), result.getPanelName());
    }

    private String normalizeLabKey(String testName, String panelName) {
        return StrUtil.blankToDefault(testName, "") + "|" + StrUtil.blankToDefault(panelName, "");
    }

    private String joinWithSeparator(String separator, String... values) {
        return Arrays.stream(values)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(separator));
    }
}
