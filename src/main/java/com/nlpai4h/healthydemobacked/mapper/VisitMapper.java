package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.Visit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 就诊表 Mapper 接口
 */
@Mapper
public interface VisitMapper extends BaseMapper<Visit> {
}
