package com.nlpai4h.healthydemobacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.mapper.NursingRecordMapper;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.NursingRecord;
import com.nlpai4h.healthydemobacked.model.vo.NursingRecordListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.INursingRecordService;
import com.nlpai4h.healthydemobacked.service.helper.PaginationHelper;
import org.springframework.stereotype.Service;
import com.nlpai4h.healthydemobacked.utils.BeanUtil;

@Service
public class NursingRecordServiceImpl extends ServiceImpl<NursingRecordMapper, NursingRecord> implements INursingRecordService {

    @Override
    public PageResult<NursingRecordListVO> pageQuery(Integer page, Integer pageSize, String registrationNo, String visitNo) {
        Page<NursingRecord> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<NursingRecord> queryWrapper = buildListQuery(registrationNo, visitNo);
        Page<NursingRecord> result = page(pageParam, queryWrapper);
        return new PageResult<>(result.getTotal(), result.getRecords().stream().map(this::toListVO).toList());
    }

    @Override
    public ScrollPageResponse<NursingRecordListVO> scrollPageQuery(String registrationNo, String visitNo, ScrollPageRequest request) {
        LambdaQueryWrapper<NursingRecord> queryWrapper = buildListQuery(registrationNo, visitNo);
        return PaginationHelper.scrollPageQuery(
                this,
                queryWrapper,
                request,
                NursingRecord::getCreateTime,
                NursingRecord::getId,
                this::toListVO
        );
    }

    private LambdaQueryWrapper<NursingRecord> buildListQuery(String registrationNo, String visitNo) {
        LambdaQueryWrapper<NursingRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                NursingRecord::getId,
                NursingRecord::getRegistrationNo,
                NursingRecord::getVisitNo,
                NursingRecord::getMeasureTime,
                NursingRecord::getItemName,
                NursingRecord::getUpperLimit,
                NursingRecord::getLowerLimit,
                NursingRecord::getMeasureValue,
                NursingRecord::getCreateTime
        );
        if (StrUtil.isNotBlank(registrationNo)) {
            queryWrapper.eq(NursingRecord::getRegistrationNo, registrationNo);
        }
        if (StrUtil.isNotBlank(visitNo)) {
            queryWrapper.eq(NursingRecord::getVisitNo, visitNo);
        }
        queryWrapper.orderByDesc(NursingRecord::getCreateTime).orderByDesc(NursingRecord::getId);
        return queryWrapper;
    }

    private NursingRecordListVO toListVO(NursingRecord record) {
        return BeanUtil.convert(record, NursingRecordListVO.class);
    }
}
