package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.mapper.AiReportMapper;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.AiReport;
import com.nlpai4h.healthydemobacked.model.vo.AiReportContextVO;
import com.nlpai4h.healthydemobacked.model.vo.AiReportVO;
import com.nlpai4h.healthydemobacked.service.IAiGenerationService;
import com.nlpai4h.healthydemobacked.service.ai.AiReportOrchestrator;
import com.nlpai4h.healthydemobacked.service.IReportService;
import com.nlpai4h.healthydemobacked.service.helper.PatientRecordSummaryHelper;
import com.nlpai4h.healthydemobacked.service.helper.VisitContextHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

import org.springframework.context.event.EventListener;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.nlpai4h.healthydemobacked.model.event.AiReportProgressEvent;

/**
 * AI报告服务实现类
 * 处理AI病历报告的查询、任务调度及SSE流式订阅逻辑
 */
@Service
@Slf4j
public class ReportServiceImpl extends ServiceImpl<AiReportMapper, AiReport> implements IReportService {

    @Resource
    private IAiGenerationService aiGenerationService;
    @Resource
    private PatientRecordSummaryHelper patientRecordSummaryHelper;
    @Resource
    private VisitContextHelper visitContextHelper;
    @Resource
    private AiReportOrchestrator aiReportOrchestrator;
    @Resource
    @Qualifier("aiTaskExecutor")
    private Executor aiTaskExecutor;

    private static class EmitterContext {
        SseEmitter emitter;
        AiReportContextVO source;
        String lastPayload;

        EmitterContext(SseEmitter emitter, AiReportContextVO source) {
            this.emitter = emitter;
            this.source = source;
        }
    }

    private final Map<String, List<EmitterContext>> emitterMap = new ConcurrentHashMap<>();

    /**
     * 查询最新的AI报告状态并拼装完整上下文
     */
    @Override
    public AiReportVO getAiReport(QueryFormDTO queryFormDTO) {
        QueryFormDTO normalized = normalizeQuery(queryFormDTO);
        AiReportContextVO source = null;
        if (Boolean.TRUE.equals(normalized.getNeedSummary()) || normalized.getNeedSummary() == null) {
            source = patientRecordSummaryHelper.buildAiReportContext(normalized);
        }
        AiReport current = aiReportOrchestrator.findLatestReport(normalized.getVisitNo(), normalized.getRegistrationNo());
        return aiReportOrchestrator.toSnapshot(current, source, source);
    }

    @Override
    @Transactional
    public void generateAiReport(QueryFormDTO queryFormDTO) {
        startReportGeneration(queryFormDTO);
    }

    @Override
    @Transactional
    public void updateReport(QueryFormDTO queryFormDTO) {
        regenerateReport(queryFormDTO);
    }

    /**
     * 启动报告生成任务
     * 如果当前已有成功或正在生成的任务，则不重复启动
     */
    @Override
    @Transactional
    public AiReportVO startReportGeneration(QueryFormDTO queryFormDTO) {
        QueryFormDTO normalized = normalizeQuery(queryFormDTO);
        // 准备任务，检查当前状态
        AiReport prepared = aiReportOrchestrator.prepareTask(normalized, false);
        String phase = aiReportOrchestrator.resolveStatusPhase(prepared);
        // 如果未成功且未在生成中，则发起异步任务
        if (!AiReportOrchestrator.PHASE_SUCCEEDED.equals(phase) && !AiReportOrchestrator.PHASE_GENERATING.equals(phase)) {
            aiGenerationService.generateReportAsync(new QueryFormDTO(
                    normalized.getVisitNo(),
                    normalized.getRegistrationNo(),
                    normalized.getNeedSummary()
            ));
        }
        AiReportContextVO source = patientRecordSummaryHelper.buildAiReportContext(normalized);
        AiReport current = aiReportOrchestrator.findLatestReport(normalized.getVisitNo(), normalized.getRegistrationNo());
        return aiReportOrchestrator.toSnapshot(current, source, source);
    }

    /**
     * 强制重置任务状态并重新开始生成报告
     */
    @Override
    @Transactional
    public AiReportVO regenerateReport(QueryFormDTO queryFormDTO) {
        QueryFormDTO normalized = normalizeQuery(queryFormDTO);
        // 强制重置状态
        aiReportOrchestrator.prepareTask(normalized, true);
        // 触发异步生成任务
        aiGenerationService.generateReportAsync(new QueryFormDTO(
                normalized.getVisitNo(),
                normalized.getRegistrationNo(),
                normalized.getNeedSummary()
        ));
        AiReportContextVO source = patientRecordSummaryHelper.buildAiReportContext(normalized);
        AiReport current = aiReportOrchestrator.findLatestReport(normalized.getVisitNo(), normalized.getRegistrationNo());
        return aiReportOrchestrator.toSnapshot(current, source, source);
    }

    /**
     * 通过SSE提供长连接支持，实时推送报告状态变更
     */
    @Override
    public SseEmitter streamAiReport(QueryFormDTO queryFormDTO) {
        QueryFormDTO normalized = normalizeQuery(queryFormDTO);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L);
        
        String key = normalized.getVisitNo() + ":" + normalized.getRegistrationNo();
        AiReportContextVO source = patientRecordSummaryHelper.buildAiReportContext(normalized);
        
        EmitterContext context = new EmitterContext(emitter, source);
        emitterMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(context);
        
        // Push initial state
        aiTaskExecutor.execute(() -> {
            try {
                AiReport current = aiReportOrchestrator.findLatestReport(normalized.getVisitNo(), normalized.getRegistrationNo());
                AiReportVO snapshot = aiReportOrchestrator.toSnapshot(current, source, null);
                String payload = buildPayload(snapshot);
                context.lastPayload = payload;
                emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
                
                if (isTerminalState(snapshot.getStatusPhase())) {
                    emitter.complete();
                    emitterMap.get(key).remove(context);
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
                emitterMap.get(key).remove(context);
            }
        });
        
        emitter.onCompletion(() -> removeEmitter(key, context));
        emitter.onTimeout(() -> removeEmitter(key, context));
        
        return emitter;
    }

    private void removeEmitter(String key, EmitterContext context) {
        emitterMap.computeIfPresent(key, (k, contexts) -> {
            contexts.remove(context);
            return contexts.isEmpty() ? null : contexts;
        });
    }

    /**
     * 规范化查询参数，补全就诊号并校验
     */
    private QueryFormDTO normalizeQuery(QueryFormDTO queryFormDTO) {
        resolveVisitNo(queryFormDTO);
        validateQuery(queryFormDTO);
        return queryFormDTO;
    }

    private void validateQuery(QueryFormDTO queryFormDTO) {
        if (StrUtil.isBlank(queryFormDTO.getRegistrationNo())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "missing registrationNo");
        }
        if (StrUtil.isBlank(queryFormDTO.getVisitNo())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "visit record not found");
        }
    }

    private void resolveVisitNo(QueryFormDTO queryFormDTO) {
        visitContextHelper.resolveVisitNo(queryFormDTO);
    }

    @EventListener
    public void handleAiReportProgressEvent(AiReportProgressEvent event) {
        String key = event.getVisitNo() + ":" + event.getRegistrationNo();
        List<EmitterContext> contexts = emitterMap.get(key);
        if (contexts == null || contexts.isEmpty()) {
            return;
        }

        AiReport current = aiReportOrchestrator.findLatestReport(event.getVisitNo(), event.getRegistrationNo());
        if (current == null) {
            return;
        }

        List<EmitterContext> deadContexts = new ArrayList<>();
        for (EmitterContext context : contexts) {
            try {
                AiReportVO snapshot = aiReportOrchestrator.toSnapshot(current, context.source, null);
                String payload = buildPayload(snapshot);
                
                if (!StrUtil.equals(payload, context.lastPayload)) {
                    context.emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
                    context.lastPayload = payload;
                }

                if (isTerminalState(snapshot.getStatusPhase())) {
                    context.emitter.complete();
                    deadContexts.add(context);
                }
            } catch (Exception ex) {
                deadContexts.add(context);
            }
        }
        
        for (EmitterContext dead : deadContexts) {
            removeEmitter(key, dead);
        }
    }

    private String buildPayload(AiReportVO snapshot) {
        return snapshot.getStatusPhase()
                + "\n" + StrUtil.blankToDefault(snapshot.getPartialContent(), "")
                + "\n" + StrUtil.blankToDefault(snapshot.getStatusMessage(), "");
    }

    private boolean isTerminalState(String phase) {
        return AiReportOrchestrator.PHASE_SUCCEEDED.equals(phase)
                || AiReportOrchestrator.PHASE_FAILED.equals(phase);
    }
}
