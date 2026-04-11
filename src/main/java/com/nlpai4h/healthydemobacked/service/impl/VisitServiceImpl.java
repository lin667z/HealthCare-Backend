package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.VisitMapper;
import com.nlpai4h.healthydemobacked.model.entity.Visit;
import com.nlpai4h.healthydemobacked.model.vo.VisitListVO;
import com.nlpai4h.healthydemobacked.service.IVisitService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitServiceImpl extends ServiceImpl<VisitMapper, Visit> implements IVisitService {

    @Override
    public PageResult<VisitListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<Visit> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Visit> queryWrapper = buildListQuery(registrationNo, visitNo);
        Page<Visit> result = page(pageParam, queryWrapper);
        List<VisitListVO> records = result.getRecords().stream().map(this::toListVO).toList();
        return new PageResult<>(result.getTotal(), records);
    }

    private LambdaQueryWrapper<Visit> buildListQuery(String registrationNo, String visitNo) {
        LambdaQueryWrapper<Visit> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                Visit::getId,
                Visit::getRegistrationNo,
                Visit::getVisitNo,
                Visit::getAgeAtVisit,
                Visit::getVisitStatus,
                Visit::getVisitType,
                Visit::getCreateTime
        );
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(Visit::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(Visit::getVisitNo, visitNo);
        }
        queryWrapper.orderByDesc(Visit::getCreateTime).orderByDesc(Visit::getId);
        return queryWrapper;
    }

    private VisitListVO toListVO(Visit visit) {
        VisitListVO vo = new VisitListVO();
        vo.setId(visit.getId());
        vo.setRegistrationNo(visit.getRegistrationNo());
        vo.setVisitNo(visit.getVisitNo());
        vo.setAgeAtVisit(visit.getAgeAtVisit());
        vo.setVisitStatus(visit.getVisitStatus());
        vo.setVisitType(visit.getVisitType());
        vo.setCreateTime(visit.getCreateTime());
        return vo;
    }
}
