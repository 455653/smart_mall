package com.smartmall.order.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smartmall.common.persistence.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商品收藏。表名 pms_ 前缀但按文档 3.2 归到 order 包：
 * 收藏是"用户对商品的行为"，与浏览、加购同属交易侧用户行为数据。
 *
 * <p>唯一键 uk_fav_user_product 让"重复收藏"变成幂等操作，插入冲突即视为已收藏。
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("pms_favorite")
public class Favorite extends BaseEntity {


    /** 收藏人 */
    private Long userId;

    /** 被收藏商品 SPU，与 user_id 组成唯一键 */
    private Long productId;

    /** 归属校验：防止 A 用户取消 B 用户的收藏 */
    public boolean ownedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    /** 是否指向同一商品，用于取消收藏前定位记录 */
    public boolean sameProduct(Long productId) {
        return this.productId != null && this.productId.equals(productId);
    }

}
