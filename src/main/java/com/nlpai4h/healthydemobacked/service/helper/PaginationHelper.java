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

public class PaginationHelper {

    public static <T, VO> ScrollPageResponse<VO> scrollPageQuery(
            IService<T> service,
            LambdaQueryWrapper<T> queryWrapper,
            ScrollPageRequest request,
            SFunction<T, LocalDateTime> createTimeFunc,
            SFunction<T, Long> idFunc,
            Function<T, VO> mapper) {

        int size = normalizeSize(request.getSize());
        applyCursor(queryWrapper, request.getCursorTime(), parseCursor(request.getCursor()), createTimeFunc, idFunc);
        queryWrapper.last("LIMIT " + (size + 1));

        List<T> rows = service.list(queryWrapper);
        return toScrollResponse(rows, size, mapper, createTimeFunc, idFunc);
    }

    private static <T> void applyCursor(LambdaQueryWrapper<T> queryWrapper, LocalDateTime cursorTime, Long cursorId,
                                        SFunction<T, LocalDateTime> createTimeFunc, SFunction<T, Long> idFunc) {
        if (cursorTime == null) {
            return;
        }
        long safeCursorId = cursorId == null ? Long.MAX_VALUE : cursorId;
        queryWrapper.and(wrapper -> wrapper
                .lt(createTimeFunc, cursorTime)
                .or(orWrapper -> orWrapper.eq(createTimeFunc, cursorTime).lt(idFunc, safeCursorId)));
    }

    private static <T, VO> ScrollPageResponse<VO> toScrollResponse(List<T> rows, int size, Function<T, VO> mapper,
                                                                   SFunction<T, LocalDateTime> createTimeFunc,
                                                                   SFunction<T, Long> idFunc) {
        ScrollPageResponse<VO> response = new ScrollPageResponse<>();
        if (rows == null || rows.isEmpty()) {
            response.setRecords(Collections.emptyList());
            response.setHasMore(false);
            response.setSize(0);
            return response;
        }
        boolean hasMore = rows.size() > size;
        List<T> pageRows = hasMore ? rows.subList(0, size) : rows;
        List<VO> records = pageRows.stream().map(mapper).toList();

        response.setRecords(records);
        response.setHasMore(hasMore);
        response.setSize(records.size());

        if (hasMore) {
            T tail = pageRows.get(pageRows.size() - 1);
            Long id = idFunc.apply(tail);
            LocalDateTime createTime = createTimeFunc.apply(tail);

            response.setNextCursor(id == null ? null : String.valueOf(id));
            response.setNextCursorTime(createTime == null ? null : createTime.toString());
        }
        return response;
    }

    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

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
