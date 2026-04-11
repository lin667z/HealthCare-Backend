package com.nlpai4h.healthydemobacked.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 * @param <T> 数据类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable {
    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页数据列表
     */
    private List<T> records;
}
