package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.service.IAiGenerationService;
import com.nlpai4h.healthydemobacked.service.ai.AiReportOrchestrator;
import com.nlpai4h.healthydemobacked.service.ai.AiReportParser;
import com.nlpai4h.healthydemobacked.service.ai.AiReportPromptBuilder;
import com.nlpai4h.healthydemobacked.model.vo.AiReportContextVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

/**
 * AI报告生成服务
 * 负责调度大模型执行病历报告的生成，并处理流式输出与进度保存
 */
@Service
@Slf4j
public class AiGenerationServiceImpl implements IAiGenerationService {

    // 触发进度刷新的字符数阈值
    private static final int FLUSH_CHAR_THRESHOLD = 160;
    // 触发进度刷新的时间间隔阈值(毫秒)
    private static final long FLUSH_INTERVAL_MS = 2000L;

    @Resource
    private ChatModel chatModel;
    @Resource
    private AiReportPromptBuilder aiReportPromptBuilder;
    @Resource
    private AiReportParser aiReportParser;
    @Resource
    private AiReportOrchestrator aiReportOrchestrator;

    /**
     * 异步生成AI报告
     *
     * @param queryFormDTO 查询条件，包含就诊号和挂号单号
     */
    @Async("aiTaskExecutor")
    @Override
    public void generateReportAsync(QueryFormDTO queryFormDTO) {
        generateInternal(queryFormDTO);
    }

    /**
     * 内部通用报告生成逻辑
     * 涵盖准备上下文、构建提示词、调用模型、流式输出、解析验证及状态落库等完整流程
     */
    private void generateInternal(QueryFormDTO queryFormDTO) {
        String visitNo = queryFormDTO.getVisitNo();
        String registrationNo = queryFormDTO.getRegistrationNo();
        StringBuilder fullReport = new StringBuilder();
        try {
            // 标记报告准备阶段
            aiReportOrchestrator.markPreparing(visitNo, registrationNo);

            // 组装上下文数据和提示词
            AiReportContextVO contextVO = aiReportPromptBuilder.buildContext(queryFormDTO);
            String promptText = aiReportPromptBuilder.buildPrompt(contextVO);
            if (StrUtil.isBlank(promptText)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "No patient context available for AI report generation");
            }

            // 标记报告生成阶段
            aiReportOrchestrator.markGenerating(visitNo, registrationNo);
            // 流式调用大模型并落库
            streamAndPersist(new Prompt(promptText), visitNo, registrationNo, fullReport);

            // 对生成的完整报告内容进行验证
            AiReportParser.ValidationResult validation = aiReportParser.validate(fullReport.toString());
            if (!validation.valid()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Output validation failed: " + String.join(", ", validation.errors()));
            }

            // 标记报告生成成功
            aiReportOrchestrator.markSucceeded(visitNo, registrationNo, fullReport.toString());
        } catch (Exception ex) {
            log.error("AI report generation failed, visitNo: {}, registrationNo: {}", visitNo, registrationNo, ex);
            // 标记报告生成失败并记录错误信息
            aiReportOrchestrator.markFailed(visitNo, registrationNo, fullReport.toString(), buildFailureMessage(ex));
        }
    }

    /**
     * 流式调用模型并执行落库逻辑
     */
    private void streamAndPersist(
            Prompt prompt,
            String visitNo,
            String registrationNo,
            StringBuilder fullReport
    ) {
        AtomicReference<Long> lastFlushTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Integer> lastFlushLength = new AtomicReference<>(0);

        try {
            chatModel.stream(prompt)
                    .doOnNext(response -> {
                        String content = response.getResult().getOutput().getText();
                        if (StrUtil.isBlank(content)) {
                            return;
                        }
                        fullReport.append(content);
                        // 满足刷新阈值时，更新数据库记录
                        flushProgressIfNeeded(visitNo, registrationNo, fullReport, lastFlushTime, lastFlushLength);
                    })
                    .blockLast();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }

        // 强制最终刷新一次记录
        flushProgressForce(visitNo, registrationNo, fullReport, lastFlushTime, lastFlushLength);
    }

    private void flushProgressIfNeeded(
            String visitNo,
            String registrationNo,
            StringBuilder fullReport,
            AtomicReference<Long> lastFlushTime,
            AtomicReference<Integer> lastFlushLength
    ) {
        int currentLength = fullReport.length();
        long now = System.currentTimeMillis();
        boolean enoughChars = currentLength - lastFlushLength.get() >= FLUSH_CHAR_THRESHOLD;
        boolean enoughTime = now - lastFlushTime.get() >= FLUSH_INTERVAL_MS;
        if (!enoughChars && !enoughTime) {
            return;
        }
        aiReportOrchestrator.updatePartial(visitNo, registrationNo, fullReport.toString());
        lastFlushTime.set(now);
        lastFlushLength.set(currentLength);
    }

    private void flushProgressForce(
            String visitNo,
            String registrationNo,
            StringBuilder fullReport,
            AtomicReference<Long> lastFlushTime,
            AtomicReference<Integer> lastFlushLength
    ) {
        aiReportOrchestrator.updatePartial(visitNo, registrationNo, fullReport.toString());
        lastFlushTime.set(System.currentTimeMillis());
        lastFlushLength.set(fullReport.length());
    }

    private String buildFailureMessage(Exception ex) {
        String message = ex.getMessage();
        if (StrUtil.isBlank(message)) {
            return AiReportOrchestrator.MESSAGE_FAILED;
        }
        return AiReportOrchestrator.MESSAGE_FAILED + ": " + message;
    }
}
