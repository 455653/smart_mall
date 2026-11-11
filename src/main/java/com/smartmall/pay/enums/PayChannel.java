package com.smartmall.pay.enums;

/** 支付渠道。库里存 name()，建表默认 MOCK。 */
public enum PayChannel {

    MOCK("模拟支付"),
    ALIPAY("支付宝"),
    WECHAT_PAY("微信支付");

    private final String label;

    PayChannel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
