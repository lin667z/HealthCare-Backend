package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.ExamReport;
import com.nlpai4h.healthydemobacked.model.vo.ExamReportListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;

/**
 * 检查报告服务接口
 */
public interface IExamReportService extends IService<ExamReport> {
    /**
     * 分页查询
     */
    PageResult<ExamReportListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo);

    ScrollPageResponse<ExamReportListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request);
}
