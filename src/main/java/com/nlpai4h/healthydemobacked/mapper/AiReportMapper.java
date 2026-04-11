package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.AiReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI病历报告数据访问接口
 * 提供与数据库中ai_report表交互的基础CRUD操作
 */
@Mapper
public interface AiReportMapper extends BaseMapper<AiReport> {
}
