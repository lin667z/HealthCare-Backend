package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.DiagnosisMapper;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.Diagnosis;
import com.nlpai4h.healthydemobacked.model.vo.DiagnosisListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.IDiagnosisService;
import com.nlpai4h.healthydemobacked.service.helper.PaginationHelper;
import org.springframework.stereotype.Service;
import com.nlpai4h.healthydemobacked.utils.BeanUtil;

@Service
public class DiagnosisServiceImpl extends ServiceImpl<DiagnosisMapper, Diagnosis> implements IDiagnosisService {

    @Override
    public PageResult<DiagnosisListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<Diagnosis> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Diagnosis> queryWrapper = buildListQuery(registrationNo, visitNo);
        Page<Diagnosis> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords().stream().map(this::toListVO).toList());
    }

    @Override
    public ScrollPageResponse<DiagnosisListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request) {
        LambdaQueryWrapper<Diagnosis> queryWrapper = buildListQuery(registrationNo, visitNo);
        return PaginationHelper.scrollPageQuery(
                this,
                queryWrapper,
                request,
                Diagnosis::getCreateTime,
                Diagnosis::getId,
                this::toListVO
        );
    }

    private LambdaQueryWrapper<Diagnosis> buildListQuery(String registrationNo, String visitNo) {
        LambdaQueryWrapper<Diagnosis> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                Diagnosis::getId,
                Diagnosis::getRegistrationNo,
                Diagnosis::getVisitNo,
                Diagnosis::getDeptName,
                Diagnosis::getIsMain,
                Diagnosis::getStatus,
                Diagnosis::getDiagCode,
                Diagnosis::getIcdCode,
                Diagnosis::getDiagType,
                Diagnosis::getDiagTime,
                Diagnosis::getDiagName,
                Diagnosis::getCreateTime
        );
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(Diagnosis::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(Diagnosis::getVisitNo, visitNo);
        }
        queryWrapper.orderByDesc(Diagnosis::getCreateTime).orderByDesc(Diagnosis::getId);
        return queryWrapper;
    }

    private DiagnosisListVO toListVO(Diagnosis diagnosis) {
        return BeanUtil.convert(diagnosis, DiagnosisListVO.class);
    }
}
