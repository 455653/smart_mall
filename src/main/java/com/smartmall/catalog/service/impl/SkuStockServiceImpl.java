package com.smartmall.catalog.service.impl;

import com.smartmall.catalog.domain.Sku;
import com.smartmall.catalog.mapper.SkuMapper;
import com.smartmall.catalog.service.SkuStockService;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.config.CacheConfig;
import com.smartmall.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务实现：条件更新是唯一真相。
 *
 * <p>核心只有一句话：<b>扣库存不是「查了再判」，而是把判断写进 UPDATE 的 where</b>。
 * 单库单表单条语句由 InnoDB 行锁保证原子性，返回 0 行就代表「没扣成」。
 *
 * <p>对应的 SQL 在 {@code resources/mapper/catalog/SkuMapper.xml}：
 * <pre>
 * update pms_sku
 *    set stock = stock - #{num}
 *  where id = #{skuId}
 *    and stock &gt;= #{num}
 *    and deleted = 0
 *    and enabled = 1
 * </pre>
 * 三个条件缺一不可：库存够、没被逻辑删除、规格仍启用。
 *
 * <p>一条容易踩的坑：扣减成功后<b>绝对不能再 setStock + updateById</b>，
 * 否则会把别人刚扣的库存冲回去，详见 {@link #deduct} 的推演。
 */
@Service                     // 注册为业务层组件
@RequiredArgsConstructor     // 构造器注入（只有一个 final 字段：skuMapper）
public class SkuStockServiceImpl implements SkuStockService {

    /** 商品详情缓存名：库存变了就要整体失效，避免详情页显示旧库存 */
    private static final String PRODUCT_DETAIL = CacheConfig.PRODUCT_DETAIL;

    private final SkuMapper skuMapper;

    /**
     * 扣减库存。
     *
     * <pre>
     * 初始库存 100
     * 线程 A：UPDATE stock = stock - 5  → DB 100→95，A 内存里仍是 100
     * 线程 B：UPDATE stock = stock - 2  → DB 95→93
     * 线程 A：若此时 sku.setStock(95) + updateById → UPDATE stock = 95
     * 结果：B 扣掉的 2 件被 A 覆盖，库存凭空多出 2 件 → 超卖
     * </pre>
     *
     * <p>所以本方法返回「扣减前的快照」（订单明细要记下单时的商品信息），
     * 而不是最新库存。要最新库存请调 {@link #getSku} 重新查。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)                              // 开启事务，抛异常即回滚
    @CacheEvict(cacheNames = PRODUCT_DETAIL, allEntries = true)                // 库存变了，商品详情缓存整体失效
    public Sku deduct(Long skuId, int quantity) {
        // 参数防御：数量必须为正，否则条件更新会变成「加库存」
        if (quantity <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "购买数量必须大于 0");
        }

        // 先查规格：既校验存在性，也拿到 spec/stock 用于拼错误提示
        Sku sku = getSku(skuId);

        // 执行条件更新：SQL 里带 stock >= #{num}，返回「受影响行数」
        int rows = skuMapper.deductStock(skuId, quantity);

        // 影响 0 行 = where 条件没满足（库存不够/已删/已停用），一律按库存不足提示
        if (rows == 0) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                    "「%s」库存不足，当前可用 %d 件".formatted(sku.getSpec(), sku.getStock()));
        }

        // 重要：条件更新已由数据库完成，这里绝不再回写实体（否则会覆盖别人的扣减）
        return sku;
    }

    /**
     * 回补库存（取消订单、退款时调用），同样是条件更新。
     *
     * <p>返回 0 行的唯一可能是「规格不存在或被停用」（回补不涉及库存上限），
     * 所以抛 {@code SKU_NOT_FOUND} 而不是库存相关错误。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = PRODUCT_DETAIL, allEntries = true)                // 库存变了，清详情缓存
    public void restore(Long skuId, int quantity) {
        // 参数防御：回补数量必须为正
        if (quantity <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "回补数量必须大于 0");
        }

        // 先查规格：存在性校验 + 拿到 spec 用于报错
        Sku sku = getSku(skuId);

        // 执行条件更新：stock = stock + n where deleted = 0
        int rows = skuMapper.restoreStock(skuId, quantity);

        // 0 行说明规格已删除，回补失败
        if (rows == 0) {
            throw new BizException(ResultCode.SKU_NOT_FOUND, "规格不存在：" + sku.getSpec());
        }
    }

    /**
     * 后台调整库存：{@code delta} 是增量。
     *
     * <p>负数走出库路径（会被「库存不足」拦住，绝不会扣成负数），正数走入库路径。
     * 用增量而不是绝对值，是为了让并发下的两次调整能叠加。
     *
     * <p>返回<b>重新查出来的</b> SKU：{@code deduct} 返回的是扣减前的实体，
     * 直接返给运营会显示旧库存，让人以为「扣失败了但提示库存不足」。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = PRODUCT_DETAIL, allEntries = true)                // 无论增还是减都要清缓存
    public Sku adjust(Long skuId, int delta) {
        // 增量 0：不需要动库，直接返回当前值
        if (delta == 0) {
            return getSku(skuId);
        }

        // 负数：取反后走扣减（复用「库存不足」校验，绝不出现负库存）
        if (delta < 0) {
            deduct(skuId, -delta);
            return getSku(skuId);
        }

        // 正数：走回补
        restore(skuId, delta);
        return getSku(skuId);
    }

    /** 查询规格，找不到即 12004 */
    @Override
    public Sku getSku(Long skuId) {
        // 按主键查：自动追加 deleted = 0，逻辑删除的规格查不到
        Sku sku = skuMapper.selectById(skuId);

        // 不存在直接抛 12004，调用方不用判空
        if (sku == null) {
            throw new BizException(ResultCode.SKU_NOT_FOUND);
        }

        return sku;
    }
}
