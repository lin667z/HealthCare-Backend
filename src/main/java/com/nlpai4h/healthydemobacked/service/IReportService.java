package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.AiReport;
import com.nlpai4h.healthydemobacked.model.vo.AiReportVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI报告服务接口
 * 定义AI病历报告的查询、生成及流式获取相关操作
 */
public interface IReportService extends IService<AiReport> {

    /**
     * 获取AI报告详情
     *
     * @param queryFormDTO 查询条件
     * @return 包含报告及上下文的视图对象
     */
    AiReportVO getAiReport(QueryFormDTO queryFormDTO);

    /**
     * 生成AI报告（无返回值，内部兼容方法）
     *
     * @param queryFormDTO 查询条件
     */
    void generateAiReport(QueryFormDTO queryFormDTO);

    /**
     * 更新或重新生成AI报告（无返回值，内部兼容方法）
     *
     * @param queryFormDTO 查询条件
     */
    void updateReport(QueryFormDTO queryFormDTO);

    /**
     * 开始异步生成AI报告并返回初始状态
     *
     * @param queryFormDTO 查询条件
     * @return 初始状态的报告视图对象
     */
    AiReportVO startReportGeneration(QueryFormDTO queryFormDTO);

    /**
     * 强制重新生成AI报告并返回初始状态
     *
     * @param queryFormDTO 查询条件
     * @return 初始状态的报告视图对象
     */
    AiReportVO regenerateReport(QueryFormDTO queryFormDTO);

    /**
     * 建立SSE连接，以流式方式持续推送报告生成进度及快照
     *
     * @param queryFormDTO 查询条件
     * @return SseEmitter流发送器
     */
    SseEmitter streamAiReport(QueryFormDTO queryFormDTO);
}
