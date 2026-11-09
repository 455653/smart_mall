package com.smartmall.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 分页返回结构（文档 14.4 / 17.14）。
 *
 * <p>MyBatis-Plus 的 {@link IPage} 页码从 1 开始，对外接口同样从 1 开始，语义一致；
 * 分页插件（PaginationInnerInterceptor）会自动拼 limit 并回填 total。
 */
public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize, long pages) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 实体分页 → VO 分页，避免把数据库实体直接暴露给前端 */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        return new PageResult<>(page.getRecords().stream().map(mapper).toList(),
                page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }
}
