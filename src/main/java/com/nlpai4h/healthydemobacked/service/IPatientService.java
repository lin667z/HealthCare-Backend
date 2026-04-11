package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.Patient;
import com.nlpai4h.healthydemobacked.model.vo.PatientRecordDetailVO;

/**
 * 患者服务接口
 * 定义患者相关业务的抽象方法
 */
public interface IPatientService extends IService<Patient> {
    
    /**
     * 分页查询患者信息
     *
     * @param page           当前页码
     * @param pageSize       每页记录数
     * @param registrationNo 挂号单号（过滤条件）
     * @return 分页结果对象
     */
    PageResult pageQuery(Integer page, Integer pageSize, String registrationNo);

    /**
     * 获取患者就诊详细记录
     * 包括基本信息、检验、检查、诊断和护理等汇总数据
     *
     * @param queryFormDTO 查询条件
     * @return 包含各项详情的视图对象
     */
    PatientRecordDetailVO getPatientRecordDetail(QueryFormDTO queryFormDTO);
}
