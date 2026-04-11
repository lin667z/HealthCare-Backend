package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.MedicalRecordFrontPageMapper;
import com.nlpai4h.healthydemobacked.model.entity.MedicalRecordFrontPage;
import com.nlpai4h.healthydemobacked.service.IMedicalRecordFrontPageService;
import org.springframework.stereotype.Service;

/**
 * 病案首页服务实现类
 */
@Service
public class MedicalRecordFrontPageServiceImpl extends ServiceImpl<MedicalRecordFrontPageMapper, MedicalRecordFrontPage> implements IMedicalRecordFrontPageService {

    @Override
    public PageResult<MedicalRecordFrontPage> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<MedicalRecordFrontPage> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<MedicalRecordFrontPage> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(MedicalRecordFrontPage::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(MedicalRecordFrontPage::getVisitNo, visitNo);
        }
        Page<MedicalRecordFrontPage> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }
}
