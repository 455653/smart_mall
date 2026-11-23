package com.smartmall.order.controller;

import com.smartmall.common.api.PageResult;
import com.smartmall.common.api.Result;
import com.smartmall.common.security.RequireLogin;
import com.smartmall.common.security.UserContext;
import com.smartmall.order.dto.CreateOrderRequest;
import com.smartmall.order.service.OrderService;
import com.smartmall.order.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口层。
 *
 * <p><b>接口清单</b>（全部需要登录）：
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>POST</td><td>/api/orders</td><td>下单，幂等</td></tr>
 *   <tr><td>GET</td><td>/api/orders</td><td>我的订单分页，可按状态过滤</td></tr>
 *   <tr><td>GET</td><td>/api/orders/{orderNo}</td><td>订单详情（含明细）</td></tr>
 *   <tr><td>POST</td><td>/api/orders/{orderNo}/cancel</td><td>取消待支付订单</td></tr>
 * </table>
 *
 * <p><b>为什么下单接口没有"商品清单"参数</b>：买什么由购物车勾选项决定，
 * 客户端只提供"收货地址 + 幂等键"。价格与库存都由服务端重新计算，
 * 前端传多少钱一概不认——这是防篡改改价的第一道闸门。
 */
@RestController
@RequestMapping("/api/orders")
@RequireLogin
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 下单。
     *
     * <p>请求示例：
     * <pre>
     * POST /api/orders
     * Authorization: Bearer eyJhbGciOi...
     *
     * { "addressId": 2120351656407924999, "requestNo": "req-20261115-0001" }
     * </pre>
     *
     * <p>{@code requestNo} 由前端生成（uuid 即可）。用户手抖点两次提交时，
     * 第二次会命中幂等直接返回同一张单，不会扣两次库存。
     *
     * <p>可能的错误码：13001 购物车为空；40400 地址不存在；12003 库存不足。
     */
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.ok(orderService.create(userId, request));
    }

    /**
     * 我的订单分页。
     *
     * <p>请求示例：
     * <pre>
     * GET /api/orders?status=WAIT_PAY&amp;page=1&amp;size=10
     * </pre>
     *
     * <p>{@code status} 可不传表示全部；列表也会带上明细快照，便于前端渲染缩略图。
     */
    @GetMapping
    public Result<PageResult<OrderVO>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {

        Long userId = UserContext.requireUserId();
        return Result.ok(orderService.page(userId, status, page, size));
    }

    /**
     * 订单详情，含下单时的商品快照。
     *
     * <p>可能的错误码：13002 订单不存在或不属于当前用户。
     */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        Long userId = UserContext.requireUserId();
        return Result.ok(orderService.detail(userId, orderNo));
    }

    /**
     * 取消订单（仅待支付可取消，取消后库存回补）。
     *
     * <p>请求示例：
     * <pre>
     * POST /api/orders/O202611152123450001/cancel
     * → { "code": 0, "message": "成功", "data": null }
     * </pre>
     *
     * <p>可能的错误码：13002 订单不存在；13003 当前状态不允许取消。
     */
    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo) {
        Long userId = UserContext.requireUserId();
        orderService.cancel(userId, orderNo);
        return Result.ok();
    }
}
