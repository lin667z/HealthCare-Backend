package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.entity.MedicalRecordFrontPage;
import com.nlpai4h.healthydemobacked.service.IMedicalRecordFrontPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 病案首页控制器
 */
@RestController
@RequestMapping("/api/medical-record-front-pages")
public class MedicalRecordFrontPageController {

    @Autowired
    private IMedicalRecordFrontPageService medicalRecordFrontPageService;

    @GetMapping("/{id}")
    public Result<MedicalRecordFrontPage> getById(@PathVariable Long id) {
        return Result.success(medicalRecordFrontPageService.getById(id));
    }

    @GetMapping
    public Result<PageResult> pageQuery(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String registrationNo,
                                        @RequestParam(required = false) String visitNo) {
        return Result.success(medicalRecordFrontPageService.pageQuery(page, pageSize, registrationNo, visitNo));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody MedicalRecordFrontPage medicalRecordFrontPage) {
        return Result.success(medicalRecordFrontPageService.save(medicalRecordFrontPage));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody MedicalRecordFrontPage medicalRecordFrontPage) {
        return Result.success(medicalRecordFrontPageService.updateById(medicalRecordFrontPage));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(medicalRecordFrontPageService.removeById(id));
    }
}
