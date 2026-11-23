package com.smartmall.cart.controller;

import com.smartmall.cart.dto.AddCartItemRequest;
import com.smartmall.cart.dto.CheckAllRequest;
import com.smartmall.cart.dto.UpdateCartItemRequest;
import com.smartmall.cart.service.CartService;
import com.smartmall.cart.vo.CartItemVO;
import com.smartmall.cart.vo.CartVO;
import com.smartmall.common.api.Result;
import com.smartmall.common.security.RequireLogin;
import com.smartmall.common.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 购物车接口层。
 *
 * <p><b>接口清单</b>（全部需要登录）：
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>GET</td><td>/api/cart</td><td>我的购物车 + 结算汇总</td></tr>
 *   <tr><td>POST</td><td>/api/cart/items</td><td>加购，同 SKU 累加</td></tr>
 *   <tr><td>PUT</td><td>/api/cart/items/{id}</td><td>改数量 / 改勾选</td></tr>
 *   <tr><td>DELETE</td><td>/api/cart/items/{id}</td><td>删除单条</td></tr>
 *   <tr><td>DELETE</td><td>/api/cart</td><td>清空</td></tr>
 *   <tr><td>PUT</td><td>/api/cart/checked</td><td>全选 / 取消全选</td></tr>
 * </table>
 *
 * <p><b>为什么每个方法都调 {@code UserContext.requireUserId()}</b>：
 * 用户 ID 只能来自令牌。如果做成「从参数里取」，前端填一个别人的 ID 就能操作别人的车。
 */
@RestController                          // REST 控制器
@RequestMapping("/api/cart")             // 公共前缀
@RequireLogin                            // 类级登录校验
@RequiredArgsConstructor                 // 构造器注入
public class CartController {

    private final CartService cartService;

    /** 我的购物车 + 结算汇总。 */
    @GetMapping
    public Result<CartVO> list() {
        Long userId = UserContext.requireUserId();
        return Result.ok(cartService.list(userId));
    }

    /** 加入购物车：同一个 SKU 重复加购是累加数量，不会产生两条记录。 */
    @PostMapping("/items")
    public Result<CartItemVO> add(@Valid @RequestBody AddCartItemRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.ok(cartService.add(userId, request));
    }

    /** 改数量或勾选状态：没传的字段不会被动。 */
    @PutMapping("/items/{id}")
    public Result<CartItemVO> update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.ok(cartService.update(userId, id, request));
    }

    /** 删除单条。 */
    @DeleteMapping("/items/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        Long userId = UserContext.requireUserId();
        cartService.remove(userId, id);
        return Result.ok();
    }

    /** 清空购物车。 */
    @DeleteMapping
    public Result<Void> clear() {
        Long userId = UserContext.requireUserId();
        cartService.clear(userId);
        return Result.ok();
    }

    /** 全选 / 取消全选。 */
    @PutMapping("/checked")
    public Result<Void> checkAll(@Valid @RequestBody CheckAllRequest request) {
        Long userId = UserContext.requireUserId();
        cartService.checkAll(userId, request.checked());
        return Result.ok();
    }
}
