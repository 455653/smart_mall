package com.smartmall.marketing.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import com.smartmall.marketing.enums.SeckillStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动。一条记录对应「某个 SKU 在某个时间段内的特价库存」。
 *
 * <p>stock / sold 是并发热点：下单走条件 UPDATE（stock - sold >= n）由行锁保证原子性，
 * 不使用「先查再改」，否则高并发下必然超卖。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("mkt_seckill")
public class Seckill extends BaseEntity {


    /** 商品 SPU ID，便于列表页一次性带出商品信息。 */
    private Long productId;

    /** 参与秒杀的 SKU ID，秒杀按 SKU 维度限库存。 */
    private Long skuId;

    /** 秒杀价，明显低于 SKU 原价，下单时作为成交价快照。 */
    private BigDecimal seckillPrice;

    /** 活动库存总量。 */
    private int stock;

    /** 已售数量，与 stock 配合判断是否售罄。 */
    private int sold;

    /** 活动开始时间。 */
    private LocalDateTime startTime;

    /** 活动结束时间。 */
    private LocalDateTime endTime;

    /** 活动状态：未开始 / 进行中 / 已结束。 */
    private SeckillStatus status;

    /** 当前是否处于活动窗口：状态进行中且系统时间落在起止时间之间。 */
    public boolean running() {
        LocalDateTime now = LocalDateTime.now();
        return status == SeckillStatus.ACTIVE
                && startTime != null && endTime != null
                && !now.isBefore(startTime) && !now.isAfter(endTime);
    }

    /** 剩余名额，售罄返回 0。 */
    public int remaining() {
        return Math.max(stock - sold, 0);
    }

    /** 是否已售罄。 */
    public boolean soldOut() {
        return remaining() <= 0;
    }

}
