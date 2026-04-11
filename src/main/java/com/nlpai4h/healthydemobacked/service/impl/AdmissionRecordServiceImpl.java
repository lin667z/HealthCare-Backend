package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.AdmissionRecordMapper;
import com.nlpai4h.healthydemobacked.model.entity.AdmissionRecord;
import com.nlpai4h.healthydemobacked.service.IAdmissionRecordService;
import org.springframework.stereotype.Service;

/**
 * 入院记录服务实现类
 */
@Service
public class AdmissionRecordServiceImpl extends ServiceImpl<AdmissionRecordMapper, AdmissionRecord> implements IAdmissionRecordService {

    @Override
    public PageResult<AdmissionRecord> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<AdmissionRecord> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<AdmissionRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(AdmissionRecord::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(AdmissionRecord::getVisitNo, visitNo);
        }
        Page<AdmissionRecord> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }
}
