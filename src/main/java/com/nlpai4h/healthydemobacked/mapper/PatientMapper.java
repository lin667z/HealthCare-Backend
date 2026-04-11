package com.nlpai4h.healthydemobacked.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nlpai4h.healthydemobacked.model.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 病人表 Mapper 接口
 */
@Mapper
public interface PatientMapper extends BaseMapper<Patient> {
}
