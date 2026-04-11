package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.annotation.NoControllerLog;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.vo.AiReportVO;
import com.nlpai4h.healthydemobacked.service.IReportService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI病历报告控制器
 * 处理与AI生成病历报告相关的HTTP请求，包括生成、重新生成、获取及流式输出
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Resource
    private IReportService reportService;

    /**
     * 获取已生成的AI报告
     *
     * @param queryFormDTO 包含就诊号等查询条件
     * @return 包含AI报告详情的统一响应对象
     */
    @GetMapping
    public Result<AiReportVO> getReport(QueryFormDTO queryFormDTO) {
        return Result.success(reportService.getAiReport(queryFormDTO));
    }

    /**
     * 开始生成AI报告
     * 触发异步报告生成任务，并返回初始状态的报告VO
     *
     * @param queryFormDTO 包含就诊号和挂号号等参数
     * @return 包含提示信息和初始报告状态的响应对象
     */
    @PostMapping("/generate")
    public Result<AiReportVO> startGenerate(@RequestBody QueryFormDTO queryFormDTO) {
        return Result.success("AI report task accepted", reportService.startReportGeneration(queryFormDTO));
    }

    /**
     * 重新生成AI报告
     * 重新触发异步报告生成任务
     *
     * @param queryFormDTO 包含就诊号和挂号号等参数
     * @return 包含提示信息和更新后报告状态的响应对象
     */
    @PostMapping("/regenerate")
    public Result<AiReportVO> regenerate(@RequestBody QueryFormDTO queryFormDTO) {
        return Result.success("AI report regeneration accepted", reportService.regenerateReport(queryFormDTO));
    }

    /**
     * 流式输出AI报告生成进度和内容
     * 建立SSE连接，实时推送生成进度
     *
     * @param queryFormDTO 包含就诊号等查询条件
     * @return 用于发送SSE事件的SseEmitter对象
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @NoControllerLog
    public SseEmitter streamReport(QueryFormDTO queryFormDTO) {
        return reportService.streamAiReport(queryFormDTO);
    }
}
