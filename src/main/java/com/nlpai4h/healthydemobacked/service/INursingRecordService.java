package com.nlpai4h.healthydemobacked.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.NursingRecord;
import com.nlpai4h.healthydemobacked.model.vo.NursingRecordListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;

/**
 * 护理记录服务接口
 */
public interface INursingRecordService extends IService<NursingRecord> {
    /**
     * 分页查询
     */
    PageResult<NursingRecordListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo);

    ScrollPageResponse<NursingRecordListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request);
}
