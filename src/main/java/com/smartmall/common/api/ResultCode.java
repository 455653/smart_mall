package com.smartmall.common.api;

/**
 * 全局统一错误码。
 *
 * <p>编码规则：<b>2 位大类 + 3 位序号</b>
 * <ul>
 *   <li>0 —— 成功</li>
 *   <li>40000 / 40100 / 40300 / 40400 / 42900 —— 通用（参数 / 鉴权 / 权限 / 不存在 / 限流）</li>
 *   <li>50000 段 —— 系统级</li>
 *   <li>11000 用户域、12000 商品域、13000 交易域、14000 营销域、15000 支付域、16000 智能域</li>
 * </ul>
 *
 * <p>为什么前端只认 code：网关、六个微服务、AI 服务共用同一份错误码，前端一套判断走到底，
 * 这也是 Day08 拆分时不需要改前端的根本原因。
 */
public enum ResultCode {

    // ---------- 通用 ----------
    SUCCESS(0, "成功"),
    BAD_REQUEST(40000, "参数校验失败"),
    UNAUTHORIZED(40100, "未登录或令牌已失效"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试"),

    // ---------- 系统 ----------
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试"),
    DEGRADED(50001, "服务暂时降级，请稍后重试"),

    // ---------- 用户域 11xxx ----------
    USER_NOT_FOUND(11001, "用户不存在"),
    USER_PASSWORD_ERROR(11002, "用户名或密码错误"),
    USER_DISABLED(11003, "账号已被禁用"),
    USERNAME_EXISTS(11004, "用户名已被注册"),
    TOKEN_INVALID(11005, "令牌无效或已过期"),

    // ---------- 商品域 12xxx ----------
    PRODUCT_NOT_FOUND(12001, "商品不存在"),
    PRODUCT_OFF_SHELF(12002, "商品已下架"),
    STOCK_NOT_ENOUGH(12003, "库存不足"),
    SKU_NOT_FOUND(12004, "商品规格不存在"),

    // ---------- 交易域 13xxx ----------
    CART_EMPTY(13001, "购物车为空"),
    ORDER_NOT_FOUND(13002, "订单不存在"),
    ORDER_STATUS_ILLEGAL(13003, "当前订单状态不允许该操作"),
    ORDER_PAY_TIMEOUT(13004, "订单支付超时，已自动关闭"),
    ORDER_AMOUNT_MISMATCH(13005, "订单金额校验不一致"),

    // ---------- 营销域 14xxx ----------
    COUPON_NOT_FOUND(14001, "优惠券不存在"),
    COUPON_NOT_AVAILABLE(14002, "优惠券不可用"),
    COUPON_ALREADY_RECEIVED(14003, "该优惠券已领取"),
    SECKILL_NOT_STARTED(14004, "秒杀未开始或已结束"),
    SECKILL_SOLD_OUT(14005, "秒杀商品已售罄"),

    // ---------- 支付域 15xxx ----------
    PAYMENT_NOT_FOUND(15001, "支付单不存在"),
    PAYMENT_ALREADY_PAID(15002, "该订单已支付"),

    // ---------- 智能域 16xxx ----------
    AI_NOT_AVAILABLE(16001, "智能客服暂不可用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
