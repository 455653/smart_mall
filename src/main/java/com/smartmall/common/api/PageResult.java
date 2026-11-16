package com.smartmall.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(long total, long pageNum, long pageSize, List<T> records) {

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records == null ? List.of() : records);
    }

    /** MyBatis-Plus 分页对象 → PageResult */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /**
     * 实体分页 → VO 分页，避免把数据库实体直接暴露给前端。
     *
     * <p>实体里有 password 这类字段，一旦直接返回就是事故；VO 是唯一对外契约。
     */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(mapper).toList());
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
