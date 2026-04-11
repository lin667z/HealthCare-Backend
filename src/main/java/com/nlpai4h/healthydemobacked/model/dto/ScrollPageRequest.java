package com.nlpai4h.healthydemobacked.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 滚动分页请求参数
 */
@Data
public class ScrollPageRequest {
    /**
     * 每页大小，默认10
     */
    private Integer size = 10;
    
    /**
     * 游标，用于定位下一页的起始位置
     * 首次请求时为空，后续请求使用上次返回的nextCursor
     */
    private String cursor;
    
    /**
     * 游标时间戳，用于基于时间的滚动分页
     */
    private LocalDateTime cursorTime;
}
