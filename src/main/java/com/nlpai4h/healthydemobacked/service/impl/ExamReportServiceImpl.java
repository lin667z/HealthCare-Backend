package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.ExamReportMapper;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.ExamReport;
import com.nlpai4h.healthydemobacked.model.vo.ExamReportListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.IExamReportService;
import com.nlpai4h.healthydemobacked.service.helper.PaginationHelper;
import org.springframework.stereotype.Service;
import com.nlpai4h.healthydemobacked.utils.BeanUtil;

@Service
public class ExamReportServiceImpl extends ServiceImpl<ExamReportMapper, ExamReport> implements IExamReportService {

    @Override
    public PageResult<ExamReportListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<ExamReport> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<ExamReport> queryWrapper = buildListQuery(registrationNo, visitNo);
        Page<ExamReport> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords().stream().map(this::toListVO).toList());
    }

    @Override
    public ScrollPageResponse<ExamReportListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request) {
        LambdaQueryWrapper<ExamReport> queryWrapper = buildListQuery(registrationNo, visitNo);
        return PaginationHelper.scrollPageQuery(
                this,
                queryWrapper,
                request,
                ExamReport::getCreateTime,
                ExamReport::getId,
                this::toListVO
        );
    }

    private LambdaQueryWrapper<ExamReport> buildListQuery(String registrationNo, String visitNo) {
        LambdaQueryWrapper<ExamReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                ExamReport::getId,
                ExamReport::getRegistrationNo,
                ExamReport::getVisitNo,
                ExamReport::getExamName,
                ExamReport::getExamType,
                ExamReport::getExamCode,
                ExamReport::getExamDate,
                ExamReport::getReportDate,
                ExamReport::getExamResult,
                ExamReport::getExamFindings,
                ExamReport::getCreateTime
        );
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(ExamReport::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(ExamReport::getVisitNo, visitNo);
        }
        queryWrapper.orderByDesc(ExamReport::getCreateTime).orderByDesc(ExamReport::getId);
        return queryWrapper;
    }

    private ExamReportListVO toListVO(ExamReport report) {
        return BeanUtil.convert(report, ExamReportListVO.class);
    }
}
