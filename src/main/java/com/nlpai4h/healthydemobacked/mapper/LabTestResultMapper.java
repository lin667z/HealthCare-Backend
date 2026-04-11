package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.LabTestResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检验化验结果表 Mapper 接口
 */
@Mapper
public interface LabTestResultMapper extends BaseMapper<LabTestResult> {
}
