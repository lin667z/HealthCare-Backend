package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.PatientMapper;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.Patient;
import com.nlpai4h.healthydemobacked.model.vo.PatientRecordDetailVO;
import com.nlpai4h.healthydemobacked.service.IPatientService;
import com.nlpai4h.healthydemobacked.service.helper.PatientRecordSummaryHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 患者服务实现类
 * 负责处理患者相关的核心业务逻辑
 */
@Service
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient> implements IPatientService {

    @Autowired
    private PatientRecordSummaryHelper patientRecordSummaryHelper;

    /**
     * 分页查询患者信息
     * 内部使用 MyBatis-Plus 提供的分页插件实现
     */
    @Override
    public PageResult<Patient> pageQuery(Integer page, Integer pageSize, String registrationNo) {
        Page<Patient> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Patient> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(Patient::getRegistrationNo, registrationNo);
        }
        Page<Patient> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /**
     * 获取患者就诊详细记录
     * 委托给 patientRecordSummaryHelper 进行多维度数据的聚合
     */
    @Override
    public PatientRecordDetailVO getPatientRecordDetail(QueryFormDTO queryFormDTO) {
        return patientRecordSummaryHelper.buildPatientRecordDetail(queryFormDTO);
    }
}
