package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.entity.AdmissionRecord;
import com.nlpai4h.healthydemobacked.service.IAdmissionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 入院记录控制器
 */
@RestController
@RequestMapping("/api/admission-records")
public class AdmissionRecordController {

    @Autowired
    private IAdmissionRecordService admissionRecordService;

    @GetMapping("/{id}")
    public Result<AdmissionRecord> getById(@PathVariable Long id) {
        return Result.success(admissionRecordService.getById(id));
    }

    @GetMapping
    public Result<PageResult> pageQuery(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String registrationNo,
                                        @RequestParam(required = false) String visitNo) {
        return Result.success(admissionRecordService.pageQuery(page, pageSize, registrationNo, visitNo));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody AdmissionRecord admissionRecord) {
        return Result.success(admissionRecordService.save(admissionRecord));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody AdmissionRecord admissionRecord) {
        return Result.success(admissionRecordService.updateById(admissionRecord));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(admissionRecordService.removeById(id));
    }
}
