package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.Diagnosis;
import com.nlpai4h.healthydemobacked.model.vo.DiagnosisListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;

/**
 * 诊断服务接口
 */
public interface IDiagnosisService extends IService<Diagnosis> {
    /**
     * 分页查询
     */
    PageResult<DiagnosisListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo);

    ScrollPageResponse<DiagnosisListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request);
}
