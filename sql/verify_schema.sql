-- =============================================================================
-- smart_mall 表结构核对脚本（配合 sql/alter_to_entity.sql 使用）
-- 用法：执行 alter_to_entity.sql 后跑本脚本，全部显示 PASS 即对齐成功
-- 目标库 : 192.168.83.129:3307 / smart_mall   (MySQL 8.0.46)
-- 说明：只读查询，不改任何数据
-- =============================================================================

USE smart_mall;

-- -----------------------------------------------------------------------------
-- 查询 1：改名列核对（期望「旧列已消失 + 新列已就位」）
--         任何一行出现 FAIL，说明那条 ALTER 没执行或执行的是别的列
-- -----------------------------------------------------------------------------
WITH rename_rule (`tbl`, `old_col`, `new_col`) AS (
    SELECT 'sys_audit_log', 'http_method', 'method'            UNION ALL
    SELECT 'sys_audit_log', 'path',         'uri'              UNION ALL
    SELECT 'sys_audit_log', 'error_message','error_msg'        UNION ALL
    SELECT 'sys_audit_log', 'duration_ms',  'cost_ms'          UNION ALL
    SELECT 'mkt_coupon_template', 'threshold',      'threshold_amount' UNION ALL
    SELECT 'mkt_coupon_template', 'total',          'total_count'      UNION ALL
    SELECT 'mkt_coupon_template', 'received',       'received_count'   UNION ALL
    SELECT 'mkt_coupon_template', 'per_user_limit', 'per_limit'        UNION ALL
    SELECT 'mkt_coupon_template', 'enabled',        'status'           UNION ALL
    SELECT 'mkt_full_reduction',  'threshold',      'threshold_amount' UNION ALL
    SELECT 'mkt_full_reduction',  'enabled',        'status'           UNION ALL
    SELECT 'mkt_seckill',         'enabled',        'status'           UNION ALL
    SELECT 'mkt_user_coupon', 'coupon_id',    'template_id'   UNION ALL
    SELECT 'mkt_user_coupon', 'receive_time', 'received_time' UNION ALL
    SELECT 'mkt_user_coupon', 'use_time',     'used_time'     UNION ALL
    SELECT 'pay_payment',     'pay_no',       'payment_no'    UNION ALL
    SELECT 'oms_order_item',  'image',        'product_image' UNION ALL
    SELECT 'oms_order_log',   'operator',     'operator_type' UNION ALL
    SELECT 'oms_cart_item',   'selected',     'checked'       UNION ALL
    SELECT 'oms_after_sale',  'amount',                'refund_amount'       UNION ALL
    SELECT 'oms_after_sale',  'previous_order_status', 'origin_order_status' UNION ALL
    SELECT 'oms_after_sale',  'audit_remark',          'handle_remark'       UNION ALL
    SELECT 'oms_after_sale',  'audit_time',            'handle_time'
),
actual AS (
    SELECT TABLE_NAME AS `tbl`, COLUMN_NAME AS `col`, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'smart_mall'
)
SELECT r.`tbl`                                                              AS `表`,
       CONCAT(r.old_col, ' -> ', r.new_col)                                 AS `改名列`,
       IF(o.col IS NULL, 'OK 已消失', 'FAIL 旧列仍在')                      AS `旧列状态`,
       IF(n.col IS NULL, 'FAIL 新列缺失', 'OK 已就位')                      AS `新列状态`,
       n.COLUMN_TYPE                                                        AS `新列类型`,
       n.COLUMN_DEFAULT                                                     AS `新列默认值`,
       IF(o.col IS NULL AND n.col IS NOT NULL, 'PASS', 'FAIL')              AS `结果`
FROM rename_rule r
LEFT JOIN actual o ON o.tbl = r.tbl AND o.col = r.old_col
LEFT JOIN actual n ON n.tbl = r.tbl AND n.col = r.new_col
ORDER BY `结果`, r.`tbl`;

-- -----------------------------------------------------------------------------
-- 查询 2：新增列核对（期望「列存在且类型匹配」）
-- -----------------------------------------------------------------------------
WITH add_rule (`tbl`, `col`, `expect_type`) AS (
    SELECT 'sys_audit_log',    'module',      'varchar(50)'   UNION ALL
    SELECT 'sys_audit_log',    'params',      'varchar(1000)' UNION ALL
    SELECT 'mkt_user_coupon',  'coupon_code', 'varchar(64)'   UNION ALL
    SELECT 'oms_order_log',    'operator_id', 'bigint'        UNION ALL
    SELECT 'oms_after_sale',   'apply_time',  'datetime'      UNION ALL
    SELECT 'pms_review',       'anonymous',   'bit(1)'
)
SELECT a.`tbl`                                                  AS tbl_name,
       a.`col`                                                  AS col_name,
       IF(c.COLUMN_NAME IS NULL, 'FAIL missing', 'OK exists')   AS col_status,
       c.COLUMN_TYPE                                            AS actual_type,
       IF(c.COLUMN_NAME IS NULL, '-',
          IF(c.COLUMN_TYPE = a.expect_type, 'OK type matches', 'WARN type mismatch')) AS type_check,
       c.IS_NULLABLE                                            AS nullable,
       c.COLUMN_DEFAULT                                         AS default_val
FROM add_rule a
LEFT JOIN information_schema.COLUMNS c
       ON c.TABLE_SCHEMA = 'smart_mall'
      AND c.TABLE_NAME   = a.`tbl`
      AND c.COLUMN_NAME  = a.`col`
ORDER BY col_status, tbl_name;

-- -----------------------------------------------------------------------------
-- 查询 3：三处「语义/可空性」改动单独确认
--   - mkt_coupon_template / mkt_full_reduction / mkt_seckill 的 status 应为 VARCHAR(20) 且带默认值
--   - mkt_seckill.name、pay_payment.order_id 应为可空（否则实体 INSERT 会失败）
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME   AS `表`,
       COLUMN_NAME  AS `列`,
       COLUMN_TYPE  AS `类型`,
       IS_NULLABLE  AS `可空`,
       COLUMN_DEFAULT AS `默认值`,
       CASE
           WHEN TABLE_NAME IN ('mkt_coupon_template','mkt_full_reduction','mkt_seckill')
                AND COLUMN_NAME = 'status'
                AND COLUMN_TYPE = 'varchar(20)'                       THEN 'PASS'
           WHEN TABLE_NAME = 'mkt_seckill'    AND COLUMN_NAME = 'name'
                AND IS_NULLABLE = 'YES'                               THEN 'PASS'
           WHEN TABLE_NAME = 'pay_payment'    AND COLUMN_NAME = 'order_id'
                AND IS_NULLABLE = 'YES'                               THEN 'PASS'
           ELSE 'FAIL'
       END AS `结果`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'smart_mall'
  AND (   (TABLE_NAME IN ('mkt_coupon_template','mkt_full_reduction','mkt_seckill')
           AND COLUMN_NAME = 'status')
       OR (TABLE_NAME = 'mkt_seckill' AND COLUMN_NAME = 'name')
       OR (TABLE_NAME = 'pay_payment' AND COLUMN_NAME = 'order_id'))
ORDER BY `结果`, TABLE_NAME;

-- -----------------------------------------------------------------------------
-- 查询 4：11 张表全列总览（肉眼看一遍，确认没有旧列残留）
-- -----------------------------------------------------------------------------
SELECT TABLE_NAME AS `表`,
       GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ', ') AS `列清单`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'smart_mall'
  AND TABLE_NAME IN ('sys_audit_log', 'mkt_coupon_template', 'mkt_full_reduction',
                     'mkt_seckill', 'mkt_user_coupon', 'pay_payment',
                     'oms_order_item', 'oms_order_log', 'oms_cart_item',
                     'oms_after_sale', 'pms_review')
GROUP BY TABLE_NAME
ORDER BY TABLE_NAME;
