package com.smartmall.catalog.service;

import com.smartmall.catalog.domain.Sku;
import com.smartmall.common.exception.BizException;

/**
 * 库存服务接口。
 *
 * <p>商城最不能出错的一段逻辑就在这里：<b>超卖</b>。
 *
 * <p>接口只暴露三个动作：扣、补、调。它们全部建立在同一条原则之上——
 * <b>把「够不够」写进 UPDATE 的 WHERE 里，靠数据库行锁保证原子性</b>，
 * 而不是「先 select 查库存、再 if 判断、最后 update」。
 *
 * <p>调用方通过异常区分成败：影响行数为 0 就代表条件没满足，实现类直接抛库存相关错误码。
 *
 * @see com.smartmall.catalog.service.impl.SkuStockServiceImpl
 */
public interface SkuStockService {

    /**
     * 扣减库存。
     *
     * <p>库存不足时抛异常，异常即回滚，不会出现「扣了一半」的中间状态。
     *
     * @param skuId    规格 ID
     * @param quantity 扣减数量，必须 &gt; 0
     * @return 扣减<b>前</b>的 SKU 快照（用于订单明细，stock 是旧值）
     * @throws BizException 数量非正数 40000；规格不存在 12004；库存不足 12003
     */
    Sku deduct(Long skuId, int quantity);

    /**
     * 回补库存，取消订单 / 退款时调用。
     *
     * <p>同样是条件更新（{@code stock = stock + ?}），并发下两次回补会叠加。
     *
     * @param skuId    规格 ID
     * @param quantity 回补数量，必须 &gt; 0
     * @throws BizException 数量非正数 40000；规格不存在或已停用 12004
     */
    void restore(Long skuId, int quantity);

    /**
     * 后台调整库存，{@code delta} 为增量。
     *
     * <p>正数入库、负数出库、0 直接返回当前值。用增量而非绝对值，
     * 是为了让两个运营同时改库存时能叠加而不是互相覆盖。
     *
     * @param skuId 规格 ID
     * @param delta 增量，可正可负
     * @return <b>调整后重新查出来</b>的 SKU（不是调整前的快照）
     * @throws BizException 扣减量超过库存时 12003
     */
    Sku adjust(Long skuId, int delta);

    /**
     * 按 ID 查规格。
     *
     * @param skuId 规格 ID
     * @return 规格实体，永不为 {@code null}
     * @throws BizException 规格不存在时抛 {@code SKU_NOT_FOUND}（12004）
     */
    Sku getSku(Long skuId);
}
