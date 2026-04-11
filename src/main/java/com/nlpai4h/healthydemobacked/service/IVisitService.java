package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.entity.Visit;
import com.nlpai4h.healthydemobacked.model.vo.VisitListVO;

/**
 * 就诊记录服务接口
 */
public interface IVisitService extends IService<Visit> {
    /**
     * 分页查询
     */
    PageResult<VisitListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo);
}
