package com.nlpai4h.healthydemobacked.service.ai;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nlpai4h.healthydemobacked.common.constant.AiReportStatusConstant;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.mapper.AiReportMapper;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.AiReport;
import com.nlpai4h.healthydemobacked.model.vo.AiReportContextVO;
import com.nlpai4h.healthydemobacked.model.vo.AiReportVO;
import com.nlpai4h.healthydemobacked.model.event.AiReportProgressEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class AiReportOrchestrator {

    public static final String PHASE_IDLE = "idle";
    public static final String PHASE_QUEUED = "queued";
    public static final String PHASE_PREPARING = "preparing";
    public static final String PHASE_GENERATING = "generating";
    public static final String PHASE_SUCCEEDED = "succeeded";
    public static final String PHASE_FAILED = "failed";

    public static final String MESSAGE_QUEUED = "AI report task accepted";
    public static final String MESSAGE_PREPARING = "Collecting clinical context";
    public static final String MESSAGE_GENERATING = "Generating clinical report";
    public static final String MESSAGE_SUCCEEDED = "AI report generated";
    public static final String MESSAGE_FAILED = "AI report generation failed";

    private static final long TASK_TIMEOUT_MINUTES = 5;

    @Resource
    private AiReportMapper aiReportMapper;
    @Resource
    private AiReportParser aiReportParser;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    public AiReport prepareTask(QueryFormDTO queryFormDTO, boolean regenerate) {
        String visitNo = queryFormDTO.getVisitNo();
        String registrationNo = queryFormDTO.getRegistrationNo();
        AiReport existing = findLatestReport(visitNo, registrationNo);
        if (existing == null) {
            return createQueuedTask(visitNo, registrationNo);
        }

        if (AiReportStatusConstant.GENERATING.equals(existing.getStatus()) && !isTimedOut(existing)) {
            if (!regenerate) {
                return existing;
            }
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "AI report is already generating");
        }

        if (!regenerate && AiReportStatusConstant.GENERATED.equals(existing.getStatus())) {
            return existing;
        }

        resetTask(existing.getVisitNo(), existing.getRegistrationNo());
        return findLatestReport(visitNo, registrationNo);
    }

    public void markPreparing(String visitNo, String registrationNo) {
        updateProgress(visitNo, registrationNo, MESSAGE_PREPARING, "");
    }

    public void markGenerating(String visitNo, String registrationNo) {
        updateProgress(visitNo, registrationNo, MESSAGE_GENERATING, "");
    }

    public void updatePartial(String visitNo, String registrationNo, String partialContent) {
        updateProgress(visitNo, registrationNo, MESSAGE_GENERATING, partialContent);
    }

    public void markSucceeded(String visitNo, String registrationNo, String reportContent) {
        aiReportMapper.update(null, new LambdaUpdateWrapper<AiReport>()
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .set(AiReport::getReport, reportContent)
                .set(AiReport::getPartialReport, reportContent)
                .set(AiReport::getStatus, AiReportStatusConstant.GENERATED)
                .set(AiReport::getStatusMessage, MESSAGE_SUCCEEDED)
                .set(AiReport::getUpdateTime, LocalDateTime.now()));
        publishEvent(visitNo, registrationNo);
    }

    public void markFailed(String visitNo, String registrationNo, String partialContent, String message) {
        LambdaUpdateWrapper<AiReport> wrapper = new LambdaUpdateWrapper<AiReport>()
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .set(AiReport::getStatus, AiReportStatusConstant.GENERATION_FAILED)
                .set(AiReport::getStatusMessage, StrUtil.blankToDefault(message, MESSAGE_FAILED))
                .set(AiReport::getUpdateTime, LocalDateTime.now());
        if (partialContent != null) {
            wrapper.set(AiReport::getPartialReport, partialContent);
        }
        aiReportMapper.update(null, wrapper);
        publishEvent(visitNo, registrationNo);
    }

    public AiReport findLatestReport(String visitNo, String registrationNo) {
        List<AiReport> reports = aiReportMapper.selectList(Wrappers.lambdaQuery(AiReport.class)
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .orderByDesc(AiReport::getUpdateTime)
                .orderByDesc(AiReport::getId)
                .last("LIMIT 1"));
        return reports.isEmpty() ? null : reports.get(0);
    }

    public String resolveStatusPhase(AiReport aiReport) {
        if (aiReport == null || aiReport.getStatus() == null || AiReportStatusConstant.NOT_GENERATED.equals(aiReport.getStatus())) {
            return PHASE_IDLE;
        }
        if (AiReportStatusConstant.GENERATED.equals(aiReport.getStatus())) {
            return PHASE_SUCCEEDED;
        }
        if (AiReportStatusConstant.GENERATION_FAILED.equals(aiReport.getStatus())) {
            return PHASE_FAILED;
        }
        String message = StrUtil.blankToDefault(aiReport.getStatusMessage(), "");
        if (MESSAGE_QUEUED.equals(message)) {
            return PHASE_QUEUED;
        }
        if (MESSAGE_PREPARING.equals(message)) {
            return PHASE_PREPARING;
        }
        return PHASE_GENERATING;
    }

    public AiReportVO toSnapshot(AiReport aiReport, AiReportContextVO sourceSnapshot, AiReportContextVO inputSummary) {
        String reportContent = aiReport == null ? "" : StrUtil.blankToDefault(aiReport.getReport(), "");
        String partialContent = aiReport == null ? "" : StrUtil.blankToDefault(aiReport.getPartialReport(), "");
        String contentForParsing = StrUtil.isNotBlank(reportContent) ? reportContent : partialContent;
        String riskLevel = aiReportParser.deriveRiskLevel(contentForParsing);
        List<String> keyFindings = aiReportParser.extractKeyFindings(contentForParsing);
        List<String> suggestions = aiReportParser.extractSuggestions(contentForParsing);

        return AiReportVO.builder()
                .reportId(aiReport == null ? null : aiReport.getId())
                .version(1)
                .reportContent(reportContent)
                .partialContent(partialContent)
                .status(resolveStatusPhase(aiReport))
                .statusPhase(resolveStatusPhase(aiReport))
                .updateTime(aiReport == null ? null : aiReport.getUpdateTime())
                .generatedAt(aiReport != null && AiReportStatusConstant.GENERATED.equals(aiReport.getStatus()) ? aiReport.getUpdateTime() : null)
                .statusMessage(resolveStatusMessage(aiReport))
                .riskLevel(riskLevel)
                .summaryCards(aiReportParser.buildSummaryCards(sourceSnapshot, riskLevel, keyFindings))
                .keyFindings(keyFindings)
                .followUpSuggestions(suggestions)
                .sourceSnapshot(sourceSnapshot)
                .inputSummary(inputSummary)
                .build();
    }

    public String resolveStatusMessage(AiReport aiReport) {
        if (aiReport == null) {
            return "AI report not generated";
        }
        return StrUtil.blankToDefault(aiReport.getStatusMessage(), MESSAGE_QUEUED);
    }

    private AiReport createQueuedTask(String visitNo, String registrationNo) {
        AiReport aiReport = AiReport.builder()
                .visitNo(visitNo)
                .registrationNo(registrationNo)
                .report("")
                .partialReport("")
                .status(AiReportStatusConstant.GENERATING)
                .statusMessage(MESSAGE_QUEUED)
                .build();
        aiReport.setCreateTime(LocalDateTime.now());
        aiReport.setUpdateTime(LocalDateTime.now());
        try {
            aiReportMapper.insert(aiReport);
            publishEvent(visitNo, registrationNo);
            return aiReport;
        } catch (DuplicateKeyException ex) {
            log.warn("Duplicate AI report task detected while creating queued task, visitNo={}, registrationNo={}", visitNo, registrationNo);
            return findLatestReport(visitNo, registrationNo);
        }
    }

    private void resetTask(String visitNo, String registrationNo) {
        aiReportMapper.update(null, new LambdaUpdateWrapper<AiReport>()
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .set(AiReport::getReport, "")
                .set(AiReport::getPartialReport, "")
                .set(AiReport::getStatus, AiReportStatusConstant.GENERATING)
                .set(AiReport::getStatusMessage, MESSAGE_QUEUED)
                .set(AiReport::getUpdateTime, LocalDateTime.now()));
        publishEvent(visitNo, registrationNo);
    }

    private boolean isTimedOut(AiReport aiReport) {
        return aiReport.getUpdateTime() == null || aiReport.getUpdateTime().plusMinutes(TASK_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    private void updateProgress(String visitNo, String registrationNo, String statusMessage, String partialContent) {
        aiReportMapper.update(null, new LambdaUpdateWrapper<AiReport>()
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .set(AiReport::getStatus, AiReportStatusConstant.GENERATING)
                .set(AiReport::getStatusMessage, statusMessage)
                .set(AiReport::getPartialReport, partialContent)
                .set(AiReport::getUpdateTime, LocalDateTime.now()));
        publishEvent(visitNo, registrationNo);
    }

    private void publishEvent(String visitNo, String registrationNo) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AiReportProgressEvent(this, visitNo, registrationNo));
        }
    }
}
