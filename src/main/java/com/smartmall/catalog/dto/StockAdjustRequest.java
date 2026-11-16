package com.smartmall.catalog.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 后台调整库存请求。
 *
 * <p>{@code delta} 为增量而不是绝对值：-50 表示出库 50 件，+100 表示入库 100 件。
 * 用增量而不是"我把库存设成 100"，是为了让并发下的两次调整能叠加，
 * 而不是后一次覆盖前一次。
 *
 * @param delta 库存增量，正数入库、负数出库
 */
public record StockAdjustRequest(

        @NotNull(message = "库存增量不能为空")
        Integer delta
) {
}
