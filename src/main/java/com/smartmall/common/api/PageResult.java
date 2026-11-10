package com.smartmall.common.api;

import java.util.List;

public record PageResult<T>(long total, long pageNum, long pageSize, List<T> records) {

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records == null ? List.of() : records);
    }

    /** 空页（查不到数据时用，前端不用判空） */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(0, pageNum, pageSize, List.of());
    }

    /** 总页数 */
    public long pages() {
        return pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
    }
}
