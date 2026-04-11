package com.nlpai4h.healthydemobacked.service;

import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI报告生成服务接口
 * 负责调度大模型执行病历报告的生成，并处理流式输出与进度保存
 */
public interface IAiGenerationService {

    /**
     * 异步生成AI报告（无SSE流式输出）
     *
     * @param queryFormDTO 查询条件，包含就诊号和挂号单号
     */
    void generateReportAsync(QueryFormDTO queryFormDTO);

    /**
     * 异步生成AI报告（支持SSE流式输出）
     *
     * @param queryFormDTO 查询条件，包含就诊号和挂号单号
     * @param emitter      SSE发送器，用于实时推送生成进度
     */
    void generateWithEmitter(QueryFormDTO queryFormDTO, SseEmitter emitter);
}
