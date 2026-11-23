package com.smartmall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.smartmall.common.api.ResultCode;
import com.smartmall.common.exception.BizException;
import com.smartmall.order.domain.Order;
import com.smartmall.order.enums.OrderStatus;
import com.smartmall.order.service.OrderService;
import com.smartmall.pay.domain.Payment;
import com.smartmall.pay.dto.CreatePaymentRequest;
import com.smartmall.pay.enums.PayChannel;
import com.smartmall.pay.enums.PaymentStatus;
import com.smartmall.pay.mapper.PaymentMapper;
import com.smartmall.pay.service.PaymentService;
import com.smartmall.pay.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 支付服务实现。
 *
 * <p>整个支付域只靠两条数据库约束保证正确性：
 * <ol>
 *   <li>{@code uk_payment_order} —— 一个订单只能有一条支付单，物理上杜绝重复建单</li>
 *   <li>{@code update ... where status = 'PENDING'} —— 回调幂等，第二次影响 0 行</li>
 * </ol>
 *
 * <p>没有分布式锁、没有 Redis：单库单表时，把状态判断写进 UPDATE 的 WHERE
 * 就已经是原子的了。加锁是因为"先查再判"没有原子性，不是因为更新本身不安全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** 支付单号里的时间部分 */
    private static final DateTimeFormatter PAY_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 后 4 位随机，降低同秒并发撞号 */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentMapper paymentMapper;

    private final OrderService orderService;

    /**
     * 创建支付单。
     *
     * <p>顺序：先确认订单还能付（存在、归属、未超时、未支付），
     * 再看有没有现成的未支付单可以复用，最后才新建。
     *
     * <p>金额<b>直接取订单的 pay_amount</b>，不做任何重算：
     * 一旦支付侧自己算一遍，就会和订单侧出现口径漂移，对账永远对不平。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO create(Long userId, CreatePaymentRequest request) {
        // 1. 取订单：内含归属校验，不是你的单直接 13002
        Order order = orderService.requireOrder(request.orderNo(), userId);

        // 2. 订单能不能付：payable() = 待支付 且 未过截止时间
        if (!order.payable()) {
            // 已发货/待收货/已完成，说明钱已经付过了
            if (order.getStatus() == OrderStatus.WAIT_SHIP
                    || order.getStatus() == OrderStatus.WAIT_RECEIVE
                    || order.getStatus() == OrderStatus.COMPLETED) {
                throw new BizException(ResultCode.PAYMENT_ALREADY_PAID);
            }
            // 还在待支付但已过期：提示用户重新下单
            if (order.expired()) {
                throw new BizException(ResultCode.ORDER_PAY_TIMEOUT);
            }
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "当前订单状态不能发起支付");
        }

        // 3. 复用已有支付单：用户关掉收银台又重开时不该新建一条
        Payment exist = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderNo, order.getOrderNo()));

        if (exist != null) {
            if (exist.paid()) {
                throw new BizException(ResultCode.PAYMENT_ALREADY_PAID);
            }
            // 失败的支付单作废，避免复用一张已失效的记录
            if (exist.getStatus() == PaymentStatus.FAILED) {
                throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "支付单已失效，请重新下单");
            }
            // 待支付 → 原样返回，金额与渠道都沿用第一次的选择
            return PaymentVO.of(exist);
        }

        // 4. 新建支付单
        Payment payment = new Payment();
        payment.setPaymentNo(nextPaymentNo());
        payment.setOrderNo(order.getOrderNo());
        payment.setOrderId(order.getId());                       // DB 该列为 NOT NULL，必须写入
        payment.setUserId(userId);
        payment.setChannel(request.channel() == null ? PayChannel.MOCK : request.channel());
        payment.setAmount(order.getPayAmount());                 // 金额唯一来源：订单实付
        payment.setStatus(PaymentStatus.PENDING);
        paymentMapper.insert(payment);

        log.info("创建支付单 paymentNo={} orderNo={} amount={} channel={}",
                payment.getPaymentNo(), payment.getOrderNo(), payment.getAmount(), payment.getChannel());

        return PaymentVO.of(payment);
    }

    /**
     * 支付回调。
     *
     * <p>幂等的关键在这一句条件更新：
     * {@code update pay_payment set status='PAID' where payment_no=? and status='PENDING'}。
     * 第一次影响 1 行，重复回调影响 0 行——0 行不代表失败，代表"已经处理过了"。
     *
     * <p>为什么先改支付单、再改订单，且放在同一个事务里：
     * 支付单是记账凭证，订单是业务状态。如果订单推进失败（比如已被超时关单），
     * 整个事务回滚，支付单保持 PENDING，渠道会继续重试，最终转人工退款——
     * 宁可"钱到了但单没推"，也不能"单推了但没记账"。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO callback(String paymentNo) {
        // 1. 支付单必须存在
        Payment payment = getByNo(paymentNo);

        // 2. 条件更新：只有 PENDING 能变成 PAID
        int rows = paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getPaymentNo, paymentNo)
                .eq(Payment::getStatus, PaymentStatus.PENDING)
                .set(Payment::getStatus, PaymentStatus.PAID)
                .set(Payment::getTradeNo, "MOCK" + System.currentTimeMillis())   // 模拟第三方交易号
                .set(Payment::getCallbackTime, LocalDateTime.now()));

        if (rows == 0) {
            // 没更新到：要么重复回调（已支付），要么支付单已失效
            Payment latest = getByNo(paymentNo);
            if (latest.paid()) {
                log.debug("重复支付回调，幂等返回 paymentNo={}", paymentNo);
                return PaymentVO.of(latest);
            }
            throw new BizException(ResultCode.ORDER_STATUS_ILLEGAL, "支付单状态不允许回调");
        }

        // 3. 推进订单状态：失败会抛异常，连带把上面的支付单更新一起回滚
        orderService.markPaid(payment.getOrderNo(), payment.getChannel());

        log.info("支付回调处理成功 paymentNo={} orderNo={}", paymentNo, payment.getOrderNo());

        // 4. 重新查一次，返回带 callbackTime / tradeNo 的最新状态
        return PaymentVO.of(getByNo(paymentNo));
    }

    @Override
    public PaymentVO detail(Long userId, String paymentNo) {
        Payment payment = getByNo(paymentNo);

        // 归属校验：支付单里有金额与交易号，不能给别人看
        if (!payment.ownedBy(userId)) {
            throw new BizException(ResultCode.PAYMENT_NOT_FOUND);
        }
        return PaymentVO.of(payment);
    }

    /** 按支付单号查，查不到即 15001。 */
    private Payment getByNo(String paymentNo) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getPaymentNo, paymentNo));

        if (payment == null) {
            throw new BizException(ResultCode.PAYMENT_NOT_FOUND);
        }
        return payment;
    }

    /** 生成支付单号：P + 秒级时间 + 4 位随机。 */
    private String nextPaymentNo() {
        return "P" + LocalDateTime.now().format(PAY_NO_TIME) + "%04d".formatted(RANDOM.nextInt(10000));
    }
}
