package com.nlpai4h.healthydemobacked.service.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nlpai4h.healthydemobacked.model.dto.ScrollPageRequest;
import com.nlpai4h.healthydemobacked.model.vo.ScrollPageResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 滚动分页（游标分页）工具类
 * 基于 MyBatis-Plus 实现，采用【创建时间 + 主键ID】双字段游标方案
 * 解决传统分页深度翻页性能问题，适用于列表流式加载场景
 */
public class PaginationHelper {

    /**
     * 核心滚动分页查询方法
     *
     * @param service       MyBatis-Plus 通用Service接口
     * @param queryWrapper  查询条件构造器
     * @param request       滚动分页请求参数（包含游标、分页大小等）
     * @param createTimeFunc 实体类创建时间字段的Getter方法引用
     * @param idFunc        实体类主键ID字段的Getter方法引用
     * @param mapper        实体对象转VO对象的转换函数
     * @return 封装好的滚动分页响应结果
     */
    public static <T, VO> ScrollPageResponse<VO> scrollPageQuery(
            IService<T> service,
            LambdaQueryWrapper<T> queryWrapper,
            ScrollPageRequest request,
            SFunction<T, LocalDateTime> createTimeFunc,
            SFunction<T, Long> idFunc,
            Function<T, VO> mapper) {

        // 标准化分页大小（处理非法参数）
        int size = normalizeSize(request.getSize());
        // 拼接游标查询条件（时间+ID）
        applyCursor(queryWrapper, request.getCursorTime(), parseCursor(request.getCursor()), createTimeFunc, idFunc);
        // 查询 size+1 条数据，用于判断是否存在下一页
        queryWrapper.last("LIMIT " + (size + 1));

        // 执行查询
        List<T> rows = service.list(queryWrapper);
        // 转换为前端需要的分页响应对象
        return toScrollResponse(rows, size, mapper, createTimeFunc, idFunc);
    }

    /**
     * 构建游标分页的查询条件
     * 核心逻辑：(创建时间 < 游标时间) OR (创建时间 = 游标时间 AND ID < 游标ID)
     *
     * @param queryWrapper  查询条件构造器
     * @param cursorTime    上一页的游标时间
     * @param cursorId      上一页的游标ID
     * @param createTimeFunc 创建时间字段Getter
     * @param idFunc        主键ID字段Getter
     * @param <T>           实体类泛型
     */
    private static <T> void applyCursor(LambdaQueryWrapper<T> queryWrapper, LocalDateTime cursorTime, Long cursorId,
                                        SFunction<T, LocalDateTime> createTimeFunc, SFunction<T, Long> idFunc) {
        // 无游标时间，代表首次查询，不拼接条件
        if (cursorTime == null) {
            return;
        }
        // 处理游标ID为空的边界情况，赋值为最大值
        long safeCursorId = cursorId == null ? Long.MAX_VALUE : cursorId;
        // 拼接游标条件
        queryWrapper.and(wrapper -> wrapper
                .lt(createTimeFunc, cursorTime)
                .or(orWrapper -> orWrapper.eq(createTimeFunc, cursorTime).lt(idFunc, safeCursorId)));
    }

    /**
     * 将数据库查询结果转换为滚动分页响应对象
     *
     * @param rows          数据库查询的原始数据
     * @param size          期望的分页大小
     * @param mapper        实体转VO函数
     * @param createTimeFunc 创建时间字段Getter
     * @param idFunc        主键ID字段Getter
     * @param <T>           实体类泛型
     * @param <VO>          视图对象泛型
     * @return 滚动分页响应
     */
    private static <T, VO> ScrollPageResponse<VO> toScrollResponse(List<T> rows, int size, Function<T, VO> mapper,
                                                                   SFunction<T, LocalDateTime> createTimeFunc,
                                                                   SFunction<T, Long> idFunc) {
        ScrollPageResponse<VO> response = new ScrollPageResponse<>();
        // 空数据处理
        if (rows == null || rows.isEmpty()) {
            response.setRecords(Collections.emptyList());
            response.setHasMore(false);
            response.setSize(0);
            return response;
        }
        // 判断是否有下一页数据（查询了size+1条，超过size则代表有更多）
        boolean hasMore = rows.size() > size;
        // 截取当前页需要展示的数据
        List<T> pageRows = hasMore ? rows.subList(0, size) : rows;
        // 实体对象转换为VO对象
        List<VO> records = pageRows.stream().map(mapper).toList();

        // 封装响应基础数据
        response.setRecords(records);
        response.setHasMore(hasMore);
        response.setSize(records.size());

        // 有下一页时，生成下一页的游标（时间+ID）
        if (hasMore) {
            T tail = pageRows.get(pageRows.size() - 1);
            Long id = idFunc.apply(tail);
            LocalDateTime createTime = createTimeFunc.apply(tail);

            response.setNextCursor(id == null ? null : String.valueOf(id));
            response.setNextCursorTime(createTime == null ? null : createTime.toString());
        }
        return response;
    }

    /**
     * 标准化分页大小
     * 规则：null/小于1 → 默认10；大于100 → 最大100；其余返回原值
     *
     * @param size 前端传入的分页大小
     * @return 标准化后的分页大小
     */
    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    /**
     * 解析游标字符串为Long类型ID
     * 处理空值、格式异常等非法情况
     *
     * @param cursor 前端传入的游标字符串
     * @return 解析后的ID，解析失败返回null
     */
    private static Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}