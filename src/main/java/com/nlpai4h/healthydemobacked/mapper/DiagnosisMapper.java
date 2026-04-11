package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.Diagnosis;
import org.apache.ibatis.annotations.Mapper;

/**
 * 诊断表 Mapper 接口
 */
@Mapper
public interface DiagnosisMapper extends BaseMapper<Diagnosis> {
}
