package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.Diagnosis;
import com.nlpai4h.healthydemobacked.model.vo.DiagnosisListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.IDiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 诊断记录控制器
 * 提供患者诊断记录的增删改查及分页/滚动分页查询接口
 */
@RestController
@RequestMapping("/api/diagnoses")
public class DiagnosisController {

    @Autowired
    private IDiagnosisService diagnosisService;

    /**
     * 根据ID获取单条诊断记录
     *
     * @param id 诊断记录ID
     * @return 包含诊断记录信息的响应结果
     */
    @GetMapping("/{id}")
    public Result<Diagnosis> getById(@PathVariable Long id) {
        return Result.success(diagnosisService.getById(id));
    }

    /**
     * 分页或滚动分页查询诊断记录
     * 如果传入了 size 参数，则使用滚动分页，否则使用标准分页
     *
     * @param page           当前页码（标准分页）
     * @param pageSize       每页记录数（标准分页）
     * @param registrationNo 挂号单号（过滤条件）
     * @param visitNo        就诊编号（过滤条件）
     * @param size           滚动分页大小
     * @param cursor         游标（滚动分页）
     * @param cursorTime     游标时间（滚动分页）
     * @return 包含分页结果的响应对象
     */
    @GetMapping
    public Result<?> pageQuery(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) String registrationNo,
                               @RequestParam(required = false) String visitNo,
                               @RequestParam(required = false) Integer size,
                               @RequestParam(required = false) String cursor,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorTime) {
        if (size != null) {
            ScrollPageRequest request = new ScrollPageRequest();
            request.setSize(size);
            request.setCursor(cursor);
            request.setCursorTime(cursorTime);
            ScrollPageResponse<DiagnosisListVO> response = diagnosisService.scrollPageQuery(registrationNo, visitNo, request);
            return Result.success(response);
        }
        PageResult<DiagnosisListVO> response = diagnosisService.pageQuery(page, pageSize, registrationNo, visitNo);
        return Result.success(response);
    }

    /**
     * 新增诊断记录
     *
     * @param diagnosis 待保存的诊断实体
     * @return 是否保存成功的响应结果
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Diagnosis diagnosis) {
        return Result.success(diagnosisService.save(diagnosis));
    }

    /**
     * 更新诊断记录
     *
     * @param diagnosis 待更新的诊断实体
     * @return 是否更新成功的响应结果
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody Diagnosis diagnosis) {
        return Result.success(diagnosisService.updateById(diagnosis));
    }

    /**
     * 删除诊断记录
     *
     * @param id 待删除的诊断记录ID
     * @return 是否删除成功的响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(diagnosisService.removeById(id));
    }
}
