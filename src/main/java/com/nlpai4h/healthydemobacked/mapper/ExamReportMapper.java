package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.ExamReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检查报告表 Mapper 接口
 */
@Mapper
public interface ExamReportMapper extends BaseMapper<ExamReport> {
}
