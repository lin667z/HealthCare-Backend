package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.LabTestResultMapper;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.LabTestResult;
import com.nlpai4h.healthydemobacked.model.vo.LabTestResultListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.ILabTestResultService;
import com.nlpai4h.healthydemobacked.service.helper.PaginationHelper;
import org.springframework.stereotype.Service;
import com.nlpai4h.healthydemobacked.utils.BeanUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LabTestResultServiceImpl extends ServiceImpl<LabTestResultMapper, LabTestResult> implements ILabTestResultService {

    @Override
    public PageResult<LabTestResultListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<LabTestResult> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<LabTestResult> queryWrapper = buildListQuery(registrationNo, visitNo);
        Page<LabTestResult> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords().stream().map(this::toListVO).toList());
    }

    @Override
    public ScrollPageResponse<LabTestResultListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request) {
        LambdaQueryWrapper<LabTestResult> queryWrapper = buildListQuery(registrationNo, visitNo);
        ScrollPageResponse<LabTestResultListVO> response = PaginationHelper.scrollPageQuery(
                this,
                queryWrapper,
                request,
                LabTestResult::getCreateTime,
                LabTestResult::getId,
                this::toListVO
        );

        if (StrUtil.isBlank(request.getCursor())) {
            LambdaQueryWrapper<LabTestResult> allWrapper = new LambdaQueryWrapper<>();
            allWrapper.select(LabTestResult::getAbnormalFlag, LabTestResult::getTestValue, LabTestResult::getReferenceRange);
            if (StrUtil.isNotBlank(registrationNo)) {
                allWrapper.eq(LabTestResult::getRegistrationNo, registrationNo);
            }
            if (StrUtil.isNotBlank(visitNo)) {
                allWrapper.eq(LabTestResult::getVisitNo, visitNo);
            }
            List<LabTestResult> allResults = this.list(allWrapper);
            long total = allResults.size();
            long abnormalCount = allResults.stream().filter(this::isAbnormal).count();

            response.setTotal(total);
            Map<String, Object> ext = new HashMap<>();
            ext.put("abnormalCount", abnormalCount);
            response.setExt(ext);
        }

        return response;
    }

    private boolean isAbnormal(LabTestResult item) {
        String flag = StrUtil.trimToEmpty(item.getAbnormalFlag()).toUpperCase();
        if (StrUtil.isNotBlank(flag) && !Arrays.asList("N", "NORMAL", "正常").contains(flag)) {
            return true;
        }
        try {
            double value = Double.parseDouble(StrUtil.trimToEmpty(item.getTestValue()));
            String range = StrUtil.trimToEmpty(item.getReferenceRange());
            if (StrUtil.isNotBlank(range)) {
                Matcher matcher = Pattern.compile("^(-?\\d+(\\.\\d+)?)\\s*[-~]\\s*(-?\\d+(\\.\\d+)?)$").matcher(range);
                if (matcher.matches()) {
                    double min = Double.parseDouble(matcher.group(1));
                    double max = Double.parseDouble(matcher.group(3));
                    return value < min || value > max;
                }
            }
        } catch (Exception e) {
            // ignore parsing errors
        }
        return false;
    }

    private LambdaQueryWrapper<LabTestResult> buildListQuery(String registrationNo, String visitNo) {
        LambdaQueryWrapper<LabTestResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                LabTestResult::getId,
                LabTestResult::getRegistrationNo,
                LabTestResult::getVisitNo,
                LabTestResult::getNormalizedCode,
                LabTestResult::getTestValue,
                LabTestResult::getSpecimenName,
                LabTestResult::getAbnormalFlag,
                LabTestResult::getTestName,
                LabTestResult::getReferenceRange,
                LabTestResult::getReportTime,
                LabTestResult::getUnit,
                LabTestResult::getPanelName,
                LabTestResult::getCreateTime
        );
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(LabTestResult::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(LabTestResult::getVisitNo, visitNo);
        }
        queryWrapper.orderByDesc(LabTestResult::getCreateTime).orderByDesc(LabTestResult::getId);
        return queryWrapper;
    }

    private LabTestResultListVO toListVO(LabTestResult result) {
        return BeanUtil.convert(result, LabTestResultListVO.class);
    }
}
