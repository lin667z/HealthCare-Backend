package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.LabTestResult;
import com.nlpai4h.healthydemobacked.model.vo.LabTestResultListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.ILabTestResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/lab-test-results")
public class LabTestResultController {

    @Autowired
    private ILabTestResultService labTestResultService;

    @GetMapping("/{id}")
    public Result<LabTestResult> getById(@PathVariable Long id) {
        return Result.success(labTestResultService.getById(id));
    }

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
            ScrollPageResponse<LabTestResultListVO> response = labTestResultService.scrollPageQuery(registrationNo, visitNo, request);
            return Result.success(response);
        }
        PageResult<LabTestResultListVO> response = labTestResultService.pageQuery(page, pageSize, registrationNo, visitNo);
        return Result.success(response);
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody LabTestResult labTestResult) {
        return Result.success(labTestResultService.save(labTestResult));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody LabTestResult labTestResult) {
        return Result.success(labTestResultService.updateById(labTestResult));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(labTestResultService.removeById(id));
    }
}
