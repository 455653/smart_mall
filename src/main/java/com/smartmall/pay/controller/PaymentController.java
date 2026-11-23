package com.smartmall.pay.controller;

import com.smartmall.common.api.Result;
import com.smartmall.common.security.RequireLogin;
import com.smartmall.common.security.UserContext;
import com.smartmall.pay.dto.CreatePaymentRequest;
import com.smartmall.pay.service.PaymentService;
import com.smartmall.pay.vo.PaymentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付接口层。
 *
 * <p><b>接口清单</b>：
 * <table border="1">
 *   <tr><th>方法</th><th>路径</th><th>登录</th><th>说明</th></tr>
 *   <tr><td>POST</td><td>/api/payments</td><td>是</td><td>创建支付单（调起收银台）</td></tr>
 *   <tr><td>GET</td><td>/api/payments/{paymentNo}</td><td>是</td><td>查询支付结果</td></tr>
 *   <tr><td>POST</td><td>/api/payments/{paymentNo}/callback</td><td><b>否</b></td><td>模拟第三方异步通知</td></tr>
 * </table>
 *
 * <p><b>为什么回调接口不要求登录</b>：它是"支付宝服务器"调过来的，那边没有用户令牌。
 * 真实场景的身份校验靠<b>签名验签</b>，不是登录态。本项目没接真实渠道，
 * 所以手工暴露一个接口来模拟——上线前它必须被验签逻辑取代，
 * 否则任何人不花钱就能把订单标成已支付。
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建支付单。
     *
     * <p>请求示例：
     * <pre>
     * POST /api/payments
     * Authorization: Bearer eyJhbGciOi...
     *
     * { "orderNo": "O202611152123450001", "channel": "MOCK" }
     * </pre>
     *
     * <p>拿到 {@code paymentNo} 后，真实业务是带着它去调支付宝/微信下单接口，
     * 前端跳收银台。这里用 MOCK 渠道，直接调下面的回调接口就算付成功。
     *
     * <p>可能的错误码：13002 订单不存在；15002 订单已支付；13004 订单已超时。
     */
    @PostMapping
    @RequireLogin
    public Result<PaymentVO> create(@Valid @RequestBody CreatePaymentRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.ok(paymentService.create(userId, request));
    }

    /**
     * 查询支付单。
     *
     * <p><b>建议前端用这个接口查结果，而不是等回调返回</b>：真实回调是浏览器无感的，
     * 用户付完款后前端轮询这个接口（两三秒一次）看是否变成 PAID。
     */
    @GetMapping("/{paymentNo}")
    @RequireLogin
    public Result<PaymentVO> detail(@PathVariable String paymentNo) {
        Long userId = UserContext.requireUserId();
        return Result.ok(paymentService.detail(userId, paymentNo));
    }

    /**
     * 模拟第三方支付回调（演示用，幂等，可重复调用）。
     *
     * <p>成功后订单会从 {@code WAIT_PAY} 变成 {@code WAIT_SHIP}，
     * 可以在 {@code GET /api/orders/{orderNo}} 上看到。
     *
     * <p>连续调两次不会报错，第二次直接返回已支付状态——这就是幂等。
     */
    @PostMapping("/{paymentNo}/callback")
    public Result<PaymentVO> callback(@PathVariable String paymentNo) {
        // 注意：这里刻意不取 userId，回调来自渠道而非用户
        return Result.ok(paymentService.callback(paymentNo));
    }
}
