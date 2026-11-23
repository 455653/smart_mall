package com.smartmall.pay.service;

import com.smartmall.common.exception.BizException;
import com.smartmall.pay.dto.CreatePaymentRequest;
import com.smartmall.pay.vo.PaymentVO;

/**
 * 支付服务接口。
 *
 * <p>三步走，对应真实支付的三个动作：
 * <ol>
 *   <li>{@link #create} —— 建支付单，返回"调起收银台需要的东西"（这里是支付单号）</li>
 *   <li>用户在支付宝/微信付款（本项目用 MOCK 渠道模拟，没有真实跳转）</li>
 *   <li>{@link #callback} —— 第三方异步通知"付成功了"，服务端改单并推进订单</li>
 * </ol>
 *
 * <p><b>为什么必须回调、不能信前端"我付好了"</b>：前端说你付了不算数，
 * 以第三方签名通知为准。本项目没接真实渠道，所以暴露一个手动回调接口来演示，
 * 真实场景里这个接口只接受渠道服务器的请求并验签。
 *
 * <p>幂等的做法见实现类：<b>条件更新 + 唯一索引</b>，不靠"先查再判"。
 */
public interface PaymentService {

    /**
     * 创建支付单。同一订单重复调用会复用它已有的未支付单，不会产生第二张。
     *
     * @param userId  当前用户 ID
     * @param request 订单号 + 渠道
     * @return 支付单
     * @throws BizException 订单不存在 13002；已支付 15002；订单已超时 13004；状态不允许 13003
     */
    PaymentVO create(Long userId, CreatePaymentRequest request);

    /**
     * 支付回调（模拟第三方通知）：把支付单置为已支付，并推进订单状态。
     *
     * <p>重复回调是常态（渠道会重试），所以必须幂等：
     * 第二次调用直接返回当前支付单，不报错、不重复加销量。
     *
     * @param paymentNo 支付单号
     * @return 处理后的支付单
     * @throws BizException 支付单不存在 15001
     */
    PaymentVO callback(String paymentNo);

    /**
     * 查询支付单（仅本人可见）。
     *
     * @param userId    当前用户 ID
     * @param paymentNo 支付单号
     * @return 支付单
     * @throws BizException 支付单不存在或不属于当前用户 15001
     */
    PaymentVO detail(Long userId, String paymentNo);
}
