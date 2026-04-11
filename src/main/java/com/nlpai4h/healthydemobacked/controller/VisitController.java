package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.entity.Visit;
import com.nlpai4h.healthydemobacked.model.vo.VisitListVO;
import com.nlpai4h.healthydemobacked.service.IVisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 就诊记录控制器
 * 提供患者就诊记录的查询、新增、修改和删除等接口
 */
@RestController
@RequestMapping("/api/visits")
public class VisitController {

    @Autowired
    private IVisitService visitService;

    /**
     * 根据ID获取单条就诊记录
     *
     * @param id 就诊记录ID
     * @return 包含就诊记录信息的响应结果
     */
    @GetMapping("/{id}")
    public Result<Visit> getById(@PathVariable Long id) {
        return Result.success(visitService.getById(id));
    }

    /**
     * 分页查询就诊记录列表
     *
     * @param page           当前页码
     * @param pageSize       每页记录数
     * @param registrationNo 挂号单号（可选过滤条件）
     * @param visitNo        就诊编号（可选过滤条件）
     * @return 包含就诊记录列表的分页响应结果
     */
    @GetMapping
    public Result<PageResult<VisitListVO>> pageQuery(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String registrationNo,
                                                     @RequestParam(required = false) String visitNo) {
        return Result.success(visitService.pageQuery(page, pageSize, registrationNo, visitNo));
    }

    /**
     * 新增就诊记录
     *
     * @param visit 就诊记录实体
     * @return 是否保存成功的响应结果
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Visit visit) {
        return Result.success(visitService.save(visit));
    }

    /**
     * 更新就诊记录
     *
     * @param visit 待更新的就诊记录实体
     * @return 是否更新成功的响应结果
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody Visit visit) {
        return Result.success(visitService.updateById(visit));
    }

    /**
     * 删除就诊记录
     *
     * @param id 待删除的就诊记录ID
     * @return 是否删除成功的响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(visitService.removeById(id));
    }
}
