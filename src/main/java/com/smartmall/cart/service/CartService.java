package com.smartmall.cart.service;

import com.smartmall.cart.dto.AddCartItemRequest;
import com.smartmall.cart.dto.UpdateCartItemRequest;
import com.smartmall.cart.vo.CartItemVO;
import com.smartmall.cart.vo.CartVO;
import com.smartmall.common.exception.BizException;
import com.smartmall.order.domain.CartItem;

import java.util.List;

/**
 * 购物车服务接口。
 *
 * <p>三条约定：
 * <ol>
 *   <li>所有方法第一个参数都是 {@code userId}，由 Controller 从令牌取，绝不从请求体取</li>
 *   <li>单条操作走 {@code getOwned(userId, id)} 双条件，只按 id 查会越权</li>
 *   <li>条目数据在 {@code oms_cart_item}，商品价格与库存<b>实时向 catalog 要</b>，不在本地冗余</li>
 * </ol>
 *
 * @see com.smartmall.cart.service.impl.CartServiceImpl
 */
public interface CartService {

    /**
     * 加入购物车。同一个 SKU 重复加购会累加数量，而不是插一条新的。
     *
     * @param userId  当前用户 ID
     * @param request 加购请求
     * @return 加购后的条目（含实时价格与库存）
     * @throws BizException 规格不存在 12004；已停用或售罄 12002；数量超库存 12003
     */
    CartItemVO add(Long userId, AddCartItemRequest request);

    /**
     * 我的购物车，新加的排前面，并带结算汇总。
     *
     * @param userId 当前用户 ID
     * @return 购物车视图；空车时 items 为空列表而不是 null
     */
    CartVO list(Long userId);

    /**
     * 改数量或勾选状态。
     *
     * @param userId  当前用户 ID
     * @param id      购物车条目 ID
     * @param request 要改的字段，null 表示不动
     * @return 修改后的条目
     * @throws BizException 条目不存在或不属于当前用户 13006；数量超库存 12003
     */
    CartItemVO update(Long userId, Long id, UpdateCartItemRequest request);

    /**
     * 删除单个条目。
     *
     * @param userId 当前用户 ID
     * @param id     购物车条目 ID
     * @throws BizException 条目不存在或不属于当前用户 13006
     */
    void remove(Long userId, Long id);

    /**
     * 清空我的购物车。
     *
     * @param userId 当前用户 ID
     */
    void clear(Long userId);

    /**
     * 全选 / 取消全选。
     *
     * @param userId  当前用户 ID
     * @param checked true 全选
     */
    void checkAll(Long userId, boolean checked);

    /**
     * 取<b>已勾选</b>的条目，供下单使用。
     *
     * <p>这是 order 包唯一的入口：order 不碰 {@code CartItemMapper}，
     * 只通过这个 Service 方法拿数据。
     *
     * @param userId 当前用户 ID
     * @return 勾选的条目；空车或全未勾选时返回空列表
     */
    List<CartItem> listChecked(Long userId);

    /**
     * 按 ID 批量删除（下单成功后清理已结算的条目）。
     *
     * <p>带上 {@code userId} 而不是只按 ID 删：即便调用方传错 ID，
     * 也删不掉别人的购物车。
     *
     * @param userId 当前用户 ID
     * @param ids    要删的条目 ID
     */
    void removeByIds(Long userId, List<Long> ids);
}
