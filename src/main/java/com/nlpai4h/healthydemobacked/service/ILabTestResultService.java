package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.LabTestResult;
import com.nlpai4h.healthydemobacked.model.vo.LabTestResultListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;

/**
 * 检验结果服务接口
 */
public interface ILabTestResultService extends IService<LabTestResult> {
    /**
     * 分页查询
     */
    PageResult<LabTestResultListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo);

    ScrollPageResponse<LabTestResultListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request);
}
