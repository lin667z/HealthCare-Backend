package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.entity.NursingRecord;
import com.nlpai4h.healthydemobacked.model.vo.NursingRecordListVO;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;
import com.nlpai4h.healthydemobacked.service.INursingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/nursing-records")
public class NursingRecordController {

    @Autowired
    private INursingRecordService nursingRecordService;

    @GetMapping("/{id}")
    public Result<NursingRecord> getById(@PathVariable Long id) {
        return Result.success(nursingRecordService.getById(id));
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
            ScrollPageResponse<NursingRecordListVO> response = nursingRecordService.scrollPageQuery(registrationNo, visitNo, request);
            return Result.success(response);
        }
        PageResult<NursingRecordListVO> response = nursingRecordService.pageQuery(page, pageSize, registrationNo, visitNo);
        return Result.success(response);
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody NursingRecord nursingRecord) {
        return Result.success(nursingRecordService.save(nursingRecord));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody NursingRecord nursingRecord) {
        return Result.success(nursingRecordService.updateById(nursingRecord));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(nursingRecordService.removeById(id));
    }
}
