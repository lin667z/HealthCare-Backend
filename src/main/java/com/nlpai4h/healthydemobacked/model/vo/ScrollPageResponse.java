package com.nlpai4h.healthydemobacked.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 滚动分页响应结果
 */
@Data
public class ScrollPageResponse<T> {
    /**
     * 当前页数据
     */
    private List<T> records;
    
    /**
     * 下一页游标，如果为null表示没有更多数据
     */
    private String nextCursor;
    
    /**
     * 下一页游标时间戳
     */
    private String nextCursorTime;
    
    /**
     * 是否有更多数据
     */
    private Boolean hasMore;
    
    /**
     * 当前页数据数量
     */
    private Integer size;

    /**
     * 总数据量（可选，供需要显示总数的场景使用）
     */
    private Long total;

    /**
     * 扩展业务数据（可选，供存放额外的聚合信息，如异常项数量）
     */
    private Map<String, Object> ext;
}
