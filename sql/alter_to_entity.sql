-- =============================================================================
-- smart_mall 表结构对齐脚本（以 Java 实体为准）
-- 目标库 : 192.168.83.129:3307 / smart_mall   (MySQL 8.0.46)
-- 生成日期: 2026-11-11
--
-- 背景：实体走 MyBatis-Plus「驼峰 -> 下划线」自动映射，而现有表是 day01 的
--       JPA 老模型，有 11 张表的列名对不上，一执行 SQL 就会抛
--       Unknown column 'xxx' in 'field list'。本脚本把"表"改成"实体"的形状。
--
-- 范围与约定：
--   1) 只覆盖 11 张不一致的表；pms_product / pms_sku / pms_favorite 字段本就一致，不动。
--   2) 19 张业务表当前全部 0 行，改 DDL 无数据迁移风险。
--   3) mapper/*.xml 里的自定义 SQL 用的是 #{} 实体属性名，只有 4 个 resultMap
--      （Product / Sku / CouponTemplate / Seckill）写了物理列名，且已经是新列名，
--      因此本脚本执行后 XML 无需再改。
--   4) 表里"多出来"的列一律保留不删（nickname/avatar/tag/enabled/remark/status_code/
--      audit_by/name/order_id）。其中两处原本是 NOT NULL 且无默认值，而实体里没有
--      对应字段，MyBatis-Plus 的 INSERT 不会写它们 -> 必然插入失败，故改为可空。
--   5) MySQL 的 DDL 不能回滚，请勿重复执行本脚本。
-- =============================================================================

USE smart_mall;

-- -----------------------------------------------------------------------------
-- ① 审计日志 sys_audit_log
--    实体: user_id, username, module, action, method, uri, params, ip,
--          success, error_msg, cost_ms
--    差异: http_method->method, path->uri, error_message->error_msg,
--          duration_ms->cost_ms；实体多 module / params；保留 status_code
-- -----------------------------------------------------------------------------
ALTER TABLE sys_audit_log
    CHANGE COLUMN `http_method`   `method`    VARCHAR(10)   NOT NULL COMMENT 'HTTP 方法：GET/POST/PUT/DELETE',
    CHANGE COLUMN `path`          `uri`       VARCHAR(255)  NOT NULL COMMENT '请求路径',
    CHANGE COLUMN `error_message` `error_msg` VARCHAR(500)  DEFAULT NULL COMMENT '失败原因摘要，成功时为空',
    CHANGE COLUMN `duration_ms`   `cost_ms`   BIGINT        DEFAULT NULL COMMENT '请求耗时（毫秒）',
    ADD COLUMN    `module`        VARCHAR(50)   DEFAULT NULL COMMENT '业务模块，如 order / marketing' AFTER `username`,
    ADD COLUMN    `params`        VARCHAR(1000) DEFAULT NULL COMMENT '请求参数摘要（敏感字段已脱敏）' AFTER `uri`;

-- -----------------------------------------------------------------------------
-- ② 优惠券模板 mkt_coupon_template
--    差异: threshold->threshold_amount, total->total_count,
--          received->received_count, per_user_limit->per_limit,
--          enabled BIT(1) -> status VARCHAR(20)
-- -----------------------------------------------------------------------------
ALTER TABLE mkt_coupon_template
    CHANGE COLUMN `threshold`      `threshold_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额，0 为无门槛',
    CHANGE COLUMN `total`          `total_count`      INT            NOT NULL DEFAULT 0 COMMENT '发行总量',
    CHANGE COLUMN `received`       `received_count`   INT            NOT NULL DEFAULT 0 COMMENT '已领数量',
    CHANGE COLUMN `per_user_limit` `per_limit`        INT            NOT NULL DEFAULT 1 COMMENT '每人限领张数',
    CHANGE COLUMN `enabled`        `status`           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/PAUSED/EXPIRED';

-- -----------------------------------------------------------------------------
-- ③ 满减规则 mkt_full_reduction
--    差异: threshold->threshold_amount, enabled -> status
-- -----------------------------------------------------------------------------
ALTER TABLE mkt_full_reduction
    CHANGE COLUMN `threshold` `threshold_amount` DECIMAL(10, 2) NOT NULL COMMENT '门槛金额',
    CHANGE COLUMN `enabled`   `status`           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/PAUSED/EXPIRED';

-- -----------------------------------------------------------------------------
-- ④ 秒杀活动 mkt_seckill
--    差异: enabled -> status；表里 name 实体没有 -> 改可空（否则 INSERT 失败）
-- -----------------------------------------------------------------------------
ALTER TABLE mkt_seckill
    CHANGE COLUMN `enabled` `status` VARCHAR(20)    NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACTIVE/ENDED',
    MODIFY COLUMN `name`    VARCHAR(64) DEFAULT NULL COMMENT '活动名称（实体未映射，改可空以免插入失败）';

-- -----------------------------------------------------------------------------
-- ⑤ 用户优惠券 mkt_user_coupon
--    差异: coupon_id->template_id, receive_time->received_time,
--          use_time->used_time；实体多 coupon_code
-- -----------------------------------------------------------------------------
ALTER TABLE mkt_user_coupon
    CHANGE COLUMN `coupon_id`    `template_id`   BIGINT      NOT NULL COMMENT '来源模板 ID',
    CHANGE COLUMN `receive_time` `received_time` DATETIME    NOT NULL COMMENT '领取时间',
    CHANGE COLUMN `use_time`     `used_time`     DATETIME    DEFAULT NULL COMMENT '核销时间，未使用时为空',
    ADD COLUMN    `coupon_code`  VARCHAR(64)     DEFAULT NULL COMMENT '券码，全局唯一' AFTER `user_id`;

-- -----------------------------------------------------------------------------
-- ⑥ 支付单 pay_payment
--    差异: pay_no->payment_no；表里 order_id 实体没有 -> 改可空（否则 INSERT 失败）
-- -----------------------------------------------------------------------------
ALTER TABLE pay_payment
    CHANGE COLUMN `pay_no`   `payment_no` VARCHAR(32) NOT NULL COMMENT '支付单号',
    MODIFY COLUMN `order_id` BIGINT DEFAULT NULL COMMENT '订单 ID（实体未映射，改可空以免插入失败）';

-- -----------------------------------------------------------------------------
-- ⑦ 订单明细 oms_order_item
--    差异: image->product_image
-- -----------------------------------------------------------------------------
ALTER TABLE oms_order_item
    CHANGE COLUMN `image` `product_image` VARCHAR(255) DEFAULT NULL COMMENT '下单时商品主图快照';

-- -----------------------------------------------------------------------------
-- ⑧ 订单状态流水 oms_order_log
--    差异: operator->operator_type；实体多 operator_id
-- -----------------------------------------------------------------------------
ALTER TABLE oms_order_log
    CHANGE COLUMN `operator`    `operator_type` VARCHAR(50) DEFAULT NULL COMMENT '操作人类型：USER/ADMIN/SYSTEM',
    ADD COLUMN    `operator_id` BIGINT      DEFAULT NULL COMMENT '操作人 ID，SYSTEM 时为空' AFTER `operator_type`;

-- -----------------------------------------------------------------------------
-- ⑨ 购物车 oms_cart_item
--    差异: selected->checked
-- -----------------------------------------------------------------------------
ALTER TABLE oms_cart_item
    CHANGE COLUMN `selected` `checked` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否勾选结算：1 勾选 0 未勾选';

-- -----------------------------------------------------------------------------
-- ⑩ 售后单 oms_after_sale
--    差异: amount->refund_amount, previous_order_status->origin_order_status,
--          audit_remark->handle_remark, audit_time->handle_time；实体多 apply_time
--    保留: audit_by
-- -----------------------------------------------------------------------------
ALTER TABLE oms_after_sale
    CHANGE COLUMN `amount`                `refund_amount`       DECIMAL(12, 2) NOT NULL COMMENT '申请退款金额',
    CHANGE COLUMN `previous_order_status` `origin_order_status` VARCHAR(20)    DEFAULT NULL COMMENT '申请前的订单状态，驳回时回滚用',
    CHANGE COLUMN `audit_remark`          `handle_remark`       VARCHAR(255)   DEFAULT NULL COMMENT '处理备注，驳回时必须写理由',
    CHANGE COLUMN `audit_time`            `handle_time`         DATETIME       DEFAULT NULL COMMENT '处理时间',
    ADD COLUMN    `apply_time`            DATETIME DEFAULT NULL COMMENT '申请时间，7 天无理由时效判断' AFTER `status`;

-- -----------------------------------------------------------------------------
-- ⑪ 商品评价 pms_review
--    差异: 实体多 anonymous（是否匿名）；保留 nickname
-- -----------------------------------------------------------------------------
ALTER TABLE pms_review
    ADD COLUMN `anonymous` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否匿名' AFTER `images`;


-- =============================================================================
-- 可选（不影响链路连通，按需执行）
-- =============================================================================
-- 索引改名，纯元数据操作；不改也能跑，只是名字看着别扭
-- ALTER TABLE pay_payment RENAME INDEX uk_pay_no TO uk_payment_no;

-- 语义上是"券码全局唯一"，需要唯一约束时再执行（当前表为空，可安全执行）
-- ALTER TABLE mkt_user_coupon ADD UNIQUE KEY uk_user_coupon_code (coupon_code);


-- =============================================================================
-- 执行后自检：确认 11 张表的列名已与实体一致
-- =============================================================================
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'smart_mall'
  AND TABLE_NAME IN ('sys_audit_log', 'mkt_coupon_template', 'mkt_full_reduction',
                     'mkt_seckill', 'mkt_user_coupon', 'pay_payment',
                     'oms_order_item', 'oms_order_log', 'oms_cart_item',
                     'oms_after_sale', 'pms_review')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
