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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.nlpai4h.healthydemobacked.model.event.AiReportProgressEvent;

import org.springframework.context.event.EventListener;

/**
 * AI报告服务实现类
 * 处理AI病历报告的查询、任务调度及SSE流式订阅逻辑
 */
@Service
@Slf4j
public class ReportServiceImpl extends ServiceImpl<AiReportMapper, AiReport> implements IReportService {

    /** SSE 心跳调度器（daemon 线程，不阻止 JVM 关闭） */
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "sse-heartbeat");
                t.setDaemon(true);
                return t;
            });
    /** 心跳间隔（秒） */
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;

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
    private java.util.concurrent.Executor aiTaskExecutor;

    private static class EmitterContext {
        final SseEmitter emitter;
        final AiReportContextVO source;
        volatile String lastPayload;

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
    public void generateAiReport(QueryFormDTO queryFormDTO) {
        startReportGeneration(queryFormDTO);
    }

    @Override
    public void updateReport(QueryFormDTO queryFormDTO) {
        regenerateReport(queryFormDTO);
    }

    /**
     * 启动报告生成任务
     * 如果当前已有成功、正在生成、排队中或准备中的任务，则不重复启动
     */
    @Override
    public AiReportVO startReportGeneration(QueryFormDTO queryFormDTO) {
        QueryFormDTO normalized = normalizeQuery(queryFormDTO);
        // 准备任务，检查当前状态
        AiReport prepared = aiReportOrchestrator.prepareTask(normalized, false);
        String phase = aiReportOrchestrator.resolveStatusPhase(prepared);
        // 仅在空闲或失败状态才发起新的异步任务，防止重复生成
        if (AiReportOrchestrator.PHASE_IDLE.equals(phase) || AiReportOrchestrator.PHASE_FAILED.equals(phase)) {
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

        // 启动心跳任务，防止长生成过程中连接超时
        ScheduledFuture<?> heartbeat = HEARTBEAT_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException ex) {
                // emitter 已关闭，忽略
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 推送初始状态快照
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

        emitter.onCompletion(() -> {
            heartbeat.cancel(true);
            removeEmitter(key, context);
        });
        emitter.onTimeout(() -> {
            heartbeat.cancel(true);
            removeEmitter(key, context);
        });

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

    /**
     * 异步监听报告进度事件，推送快照到已注册的SSE连接
     * 使用 @Async 避免阻塞 AI 生成线程
     */
    @Async("aiTaskExecutor")
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
