package com.smartmall.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/**
 * 购物车条目。表前缀 oms_，随交易域归 order-service（mall_order 库）。
 *
 * <p>为什么购物车不放 Redis：Day02 先保证"重启不丢"，且唯一键
 * uk_cart_user_sku 让"重复加购"退化成数量累加，业务代码不必先查后插。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("oms_cart_item")
public class CartItem extends BaseEntity {
    /** 购物车归属用户，所有读写都必须带此条件做越权隔离 */
    private Long userId;

    /** 商品 SPU，加购时冗余，方便直接跳详情页而不必回查 SKU */
    private Long productId;

    /** 商品 SKU，价格的唯一来源；与 user_id 组成唯一键 */
    private Long skuId;

    /** 购买数量，重复加购时累加；下单成功后整行删除 */
    private int quantity;

    /** 是否勾选结算，1 勾选 0 未勾选，结算页只汇总勾选项 */
    private boolean checked;

    /** 归属校验：防止 A 用户改 id 操作 B 用户的购物车 */
    public boolean ownedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /** 再次加购时的数量累加，下限保护到 1，避免脏数据产生 0 件条目 */
    public void increaseQuantity(int delta) {
        this.quantity = Math.max(1, this.quantity + delta);
    }
}
