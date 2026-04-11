package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.AdmissionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入院记录表 Mapper 接口
 */
@Mapper
public interface AdmissionRecordMapper extends BaseMapper<AdmissionRecord> {
}
