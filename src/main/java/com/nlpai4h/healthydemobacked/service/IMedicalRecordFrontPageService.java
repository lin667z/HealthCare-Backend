package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.entity.MedicalRecordFrontPage;

/**
 * 病案首页服务接口
 */
public interface IMedicalRecordFrontPageService extends IService<MedicalRecordFrontPage> {
    /**
     * 分页查询
     */
    PageResult pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo);
}
