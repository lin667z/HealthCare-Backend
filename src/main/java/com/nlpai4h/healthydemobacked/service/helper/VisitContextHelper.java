package com.nlpai4h.healthydemobacked.service.helper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.mapper.MedicalRecordFrontPageMapper;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.MedicalRecordFrontPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VisitContextHelper {

    @Autowired
    private MedicalRecordFrontPageMapper medicalRecordFrontPageMapper;

    public void resolveVisitNo(QueryFormDTO queryFormDTO) {
        String visitNo = queryFormDTO.getVisitNo();
        String registrationNo = queryFormDTO.getRegistrationNo();
        if (StrUtil.isNotBlank(visitNo)) {
            return;
        }
        if (StrUtil.isBlank(registrationNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登记号不能为空");
        }
        MedicalRecordFrontPage latestPage = medicalRecordFrontPageMapper.selectOne(Wrappers.lambdaQuery(MedicalRecordFrontPage.class)
                .eq(MedicalRecordFrontPage::getRegistrationNo, registrationNo)
                .orderByDesc(MedicalRecordFrontPage::getCreateTime)
                .orderByDesc(MedicalRecordFrontPage::getId)
                .last("LIMIT 1"));
        if (latestPage == null || StrUtil.isBlank(latestPage.getVisitNo())) {
            return;
        }
        queryFormDTO.setVisitNo(latestPage.getVisitNo());
    }
}
