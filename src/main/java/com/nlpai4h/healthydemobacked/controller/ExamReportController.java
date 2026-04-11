package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.ExamReport;
import com.nlpai4h.healthydemobacked.model.vo.ExamReportListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.IExamReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/exam-reports")
public class ExamReportController {

    @Autowired
    private IExamReportService examReportService;

    @GetMapping("/{id}")
    public Result<ExamReport> getById(@PathVariable Long id) {
        return Result.success(examReportService.getById(id));
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
            ScrollPageResponse<ExamReportListVO> response = examReportService.scrollPageQuery(registrationNo, visitNo, request);
            return Result.success(response);
        }
        PageResult<ExamReportListVO> response = examReportService.pageQuery(page, pageSize, registrationNo, visitNo);
        return Result.success(response);
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody ExamReport examReport) {
        return Result.success(examReportService.save(examReport));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody ExamReport examReport) {
        return Result.success(examReportService.updateById(examReport));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(examReportService.removeById(id));
    }
}
