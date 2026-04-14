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

/**
 * AI报告任务编排器
 * 核心职责：管理AI报告生成任务的全生命周期
 * 包含：任务创建、状态更新、进度推送、结果处理、任务重置/超时判断
 */
@Component
@Slf4j
public class AiReportOrchestrator {

    // ==================== 任务状态阶段常量（前端展示用） ====================
    /** 空闲状态：未生成报告 */
    public static final String PHASE_IDLE = "idle";
    /** 排队中：任务已接收，等待处理 */
    public static final String PHASE_QUEUED = "queued";
    /** 准备中：正在收集临床上下文数据 */
    public static final String PHASE_PREPARING = "preparing";
    /** 生成中：正在生成临床报告 */
    public static final String PHASE_GENERATING = "generating";
    /** 已完成：报告生成成功 */
    public static final String PHASE_SUCCEEDED = "succeeded";
    /** 已失败：报告生成失败 */
    public static final String PHASE_FAILED = "failed";

    // ==================== 任务状态提示信息常量 ====================
    /** 排队中提示语 */
    public static final String MESSAGE_QUEUED = "AI report task accepted";
    /** 准备中提示语 */
    public static final String MESSAGE_PREPARING = "Collecting clinical context";
    /** 生成中提示语 */
    public static final String MESSAGE_GENERATING = "Generating clinical report";
    /** 生成成功提示语 */
    public static final String MESSAGE_SUCCEEDED = "AI report generated";
    /** 生成失败提示语 */
    public static final String MESSAGE_FAILED = "AI report generation failed";

    /** 任务超时阈值：生成任务超过5分钟未完成则判定超时 */
    private static final long TASK_TIMEOUT_MINUTES = 5;

    // ==================== 依赖注入 ====================
    /** AI报告数据库操作Mapper */
    @Resource
    private AiReportMapper aiReportMapper;
    /** AI报告解析器：解析报告内容、提取风险等级/关键信息/建议 */
    @Resource
    private AiReportParser aiReportParser;
    /** Spring事件发布器：发布报告进度变更事件 */
    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 准备AI报告生成任务
     * 处理逻辑：查询历史任务 → 复用/重置/新建任务 → 支持强制重生成
     * @param queryFormDTO 查询参数（就诊号+登记号）
     * @param regenerate 是否强制重新生成报告
     * @return 处理后的AI报告任务实体
     */
    public AiReport prepareTask(QueryFormDTO queryFormDTO, boolean regenerate) {
        String visitNo = queryFormDTO.getVisitNo();
        String registrationNo = queryFormDTO.getRegistrationNo();
        // 查询当前最新的报告记录
        AiReport existing = findLatestReport(visitNo, registrationNo);
        // 无历史记录 → 创建新的排队任务
        if (existing == null) {
            return createQueuedTask(visitNo, registrationNo);
        }

        // 任务正在生成中 且 未超时
        if (AiReportStatusConstant.GENERATING.equals(existing.getStatus()) && !isTimedOut(existing)) {
            // 不重生成 → 直接返回原任务
            if (!regenerate) {
                return existing;
            }
            // 强制重生成 → 抛出异常
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "AI report is already generating");
        }

        // 报告已生成完成 且 不强制重生成 → 直接返回
        if (!regenerate && AiReportStatusConstant.GENERATED.equals(existing.getStatus())) {
            return existing;
        }

        // 重置任务状态，重新开始生成
        resetTask(existing.getVisitNo(), existing.getRegistrationNo());
        return findLatestReport(visitNo, registrationNo);
    }

    /**
     * 标记任务为【准备中】状态
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     */
    public void markPreparing(String visitNo, String registrationNo) {
        updateProgress(visitNo, registrationNo, MESSAGE_PREPARING, "");
    }

    /**
     * 标记任务为【生成中】状态
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     */
    public void markGenerating(String visitNo, String registrationNo) {
        updateProgress(visitNo, registrationNo, MESSAGE_GENERATING, "");
    }

    /**
     * 更新流式生成的【报告部分内容】
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     * @param partialContent 报告片段内容
     */
    public void updatePartial(String visitNo, String registrationNo, String partialContent) {
        updateProgress(visitNo, registrationNo, MESSAGE_GENERATING, partialContent);
    }

    /**
     * 标记任务为【生成成功】，保存完整报告
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     * @param reportContent 完整报告内容
     */
    public void markSucceeded(String visitNo, String registrationNo, String reportContent) {
        aiReportMapper.update(null, new LambdaUpdateWrapper<AiReport>()
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .set(AiReport::getReport, reportContent)
                .set(AiReport::getPartialReport, reportContent)
                .set(AiReport::getStatus, AiReportStatusConstant.GENERATED)
                .set(AiReport::getStatusMessage, MESSAGE_SUCCEEDED)
                .set(AiReport::getUpdateTime, LocalDateTime.now()));
        // 发布进度变更事件
        publishEvent(visitNo, registrationNo);
    }

    /**
     * 标记任务为【生成失败】，保存失败信息
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     * @param partialContent 生成失败前的报告片段
     * @param message 失败原因
     */
    public void markFailed(String visitNo, String registrationNo, String partialContent, String message) {
        LambdaUpdateWrapper<AiReport> wrapper = new LambdaUpdateWrapper<AiReport>()
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .set(AiReport::getStatus, AiReportStatusConstant.GENERATION_FAILED)
                .set(AiReport::getStatusMessage, StrUtil.blankToDefault(message, MESSAGE_FAILED))
                .set(AiReport::getUpdateTime, LocalDateTime.now());
        // 存在报告片段则更新
        if (partialContent != null) {
            wrapper.set(AiReport::getPartialReport, partialContent);
        }
        aiReportMapper.update(null, wrapper);
        // 发布进度变更事件
        publishEvent(visitNo, registrationNo);
    }

    /**
     * 查询最新的AI报告记录
     * 排序规则：更新时间倒序 → ID倒序，取第一条
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     * @return 最新报告实体，无则返回null
     */
    public AiReport findLatestReport(String visitNo, String registrationNo) {
        List<AiReport> reports = aiReportMapper.selectList(Wrappers.lambdaQuery(AiReport.class)
                .eq(AiReport::getVisitNo, visitNo)
                .eq(AiReport::getRegistrationNo, registrationNo)
                .orderByDesc(AiReport::getUpdateTime)
                .orderByDesc(AiReport::getId)
                .last("LIMIT 1"));
        return reports.isEmpty() ? null : reports.get(0);
    }

    /**
     * 解析报告状态 → 转换为前端识别的阶段值
     * @param aiReport 报告实体
     * @return 前端状态阶段字符串
     */
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

    /**
     * 构建前端展示用的报告VO对象
     * 整合：报告内容、解析结果、上下文数据、状态信息
     * @param aiReport 报告实体
     * @param sourceSnapshot 原始数据快照
     * @param inputSummary 输入数据摘要
     * @return 前端展示VO
     */
    public AiReportVO toSnapshot(AiReport aiReport, AiReportContextVO sourceSnapshot, AiReportContextVO inputSummary) {
        String reportContent = aiReport == null ? "" : StrUtil.blankToDefault(aiReport.getReport(), "");
        String partialContent = aiReport == null ? "" : StrUtil.blankToDefault(aiReport.getPartialReport(), "");
        // 解析优先级：完整报告 > 片段内容
        String contentForParsing = StrUtil.isNotBlank(reportContent) ? reportContent : partialContent;
        // 解析报告核心信息
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

    /**
     * 获取报告状态提示信息
     * @param aiReport 报告实体
     * @return 状态提示语
     */
    public String resolveStatusMessage(AiReport aiReport) {
        if (aiReport == null) {
            return "AI report not generated";
        }
        return StrUtil.blankToDefault(aiReport.getStatusMessage(), MESSAGE_QUEUED);
    }

    /**
     * 创建【排队中】的新报告任务
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     * @return 新建的任务实体
     */
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
            // 唯一键冲突：任务已存在，返回最新记录
            log.warn("Duplicate AI report task detected while creating queued task, visitNo={}, registrationNo={}", visitNo, registrationNo);
            return findLatestReport(visitNo, registrationNo);
        }
    }

    /**
     * 重置报告任务：清空内容、重置为排队状态
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     */
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

    /**
     * 判断任务是否超时
     * @param aiReport 报告实体
     * @return true=超时，false=未超时
     */
    private boolean isTimedOut(AiReport aiReport) {
        return aiReport.getUpdateTime() == null || aiReport.getUpdateTime().plusMinutes(TASK_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    /**
     * 统一更新任务进度（核心方法）
     * 更新：状态、提示语、片段内容、更新时间
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     * @param statusMessage 状态提示语
     * @param partialContent 报告片段
     */
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

    /**
     * 发布AI报告进度事件
     * 通知其他模块任务状态发生变更
     * @param visitNo 就诊号
     * @param registrationNo 登记号
     */
    private void publishEvent(String visitNo, String registrationNo) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new AiReportProgressEvent(this, visitNo, registrationNo));
        }
    }
}