package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.NursingRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 护理记录表 Mapper 接口
 */
@Mapper
public interface NursingRecordMapper extends BaseMapper<NursingRecord> {
}
