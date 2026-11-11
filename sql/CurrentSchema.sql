create table ai_chat_feedback
(
    id          bigint auto_increment
        primary key,
    rating      varchar(10)      not null,
    message_id  bigint           not null,
    session_id  varchar(36)      not null,
    user_id     bigint           not null,
    reason      varchar(255)     null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '回答反馈' charset = utf8mb4;

create index idx_feedback_session
    on ai_chat_feedback (session_id);

create table ai_chat_message
(
    id          bigint auto_increment
        primary key,
    session_id  varchar(36)      not null,
    user_id     bigint           not null,
    role        varchar(20)      not null,
    content     text             not null,
    citations   text             null,
    latency_ms  bigint           null,
    model       varchar(50)      null,
    transferred bit default b'0' not null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '会话消息' charset = utf8mb4;

create index idx_chat_message_session
    on ai_chat_message (session_id);

create table ai_chat_session
(
    id               bigint auto_increment
        primary key,
    session_id       varchar(36)              not null,
    user_id          bigint                   not null,
    title            varchar(100)             null,
    status           varchar(20) default 'AI' not null,
    message_count    int         default 0    not null,
    last_active_time datetime                 null,
    create_time      datetime                 not null,
    update_time      datetime                 not null,
    deleted          bit         default b'0' not null,
    constraint uk_chat_session_no
        unique (session_id)
)
    comment 'AI 会话' charset = utf8mb4;

create index idx_chat_session_user
    on ai_chat_session (user_id);

create table ai_knowledge_chunk
(
    id              bigint auto_increment
        primary key,
    doc_id          bigint           not null,
    chunk_index     int              not null,
    content         text             not null,
    vector          longtext         not null comment '逗号分隔向量',
    embedding_model varchar(50)      null,
    dimension       int              not null,
    create_time     datetime         not null,
    update_time     datetime         not null,
    deleted         bit default b'0' not null
)
    comment '知识片段' charset = utf8mb4;

create index idx_chunk_doc
    on ai_knowledge_chunk (doc_id);

create table ai_knowledge_doc
(
    id            bigint auto_increment
        primary key,
    title         varchar(128)                  not null,
    category      varchar(50)                   null,
    content       text                          not null,
    status        varchar(20) default 'PENDING' not null,
    chunk_count   int         default 0         not null,
    error_message varchar(500)                  null,
    ingested_at   datetime                      null,
    create_time   datetime                      not null,
    update_time   datetime                      not null,
    deleted       bit         default b'0'      not null
)
    comment '知识文档' charset = utf8mb4;

create table mkt_coupon_template
(
    id               bigint auto_increment
        primary key,
    name             varchar(64)                     not null,
    type             varchar(20)                     not null comment 'FULL_CUT/DISCOUNT',
    threshold_amount decimal(10, 2) default 0.00     not null comment '使用门槛金额，0 为无门槛',
    discount_amount  decimal(10, 2)                  null,
    discount_rate    decimal(4, 2)                   null,
    total_count      int            default 0        not null comment '发行总量',
    received_count   int            default 0        not null comment '已领数量',
    per_limit        int            default 1        not null comment '每人限领张数',
    start_time       datetime                        not null,
    end_time         datetime                        not null,
    status           varchar(20)    default 'ACTIVE' not null comment '状态：ACTIVE/PAUSED/EXPIRED',
    create_time      datetime                        not null,
    update_time      datetime                        not null,
    deleted          bit            default b'0'     not null
)
    comment '优惠券模板' charset = utf8mb4;

create table mkt_full_reduction
(
    id               bigint auto_increment
        primary key,
    name             varchar(64)                  not null,
    threshold_amount decimal(10, 2)               not null comment '门槛金额',
    reduce_amount    decimal(10, 2)               not null,
    start_time       datetime                     not null,
    end_time         datetime                     not null,
    status           varchar(20) default 'ACTIVE' not null comment '状态：ACTIVE/PAUSED/EXPIRED',
    create_time      datetime                     not null,
    update_time      datetime                     not null,
    deleted          bit         default b'0'     not null
)
    comment '满减规则' charset = utf8mb4;

create table mkt_seckill
(
    id            bigint auto_increment
        primary key,
    name          varchar(64)                   null comment '活动名称（实体未映射，改可空以免插入失败）',
    product_id    bigint                        not null,
    sku_id        bigint                        not null,
    seckill_price decimal(10, 2)                not null,
    stock         int                           not null,
    sold          int         default 0         not null,
    start_time    datetime                      not null,
    end_time      datetime                      not null,
    status        varchar(20) default 'PENDING' not null comment '状态：PENDING/ACTIVE/ENDED',
    create_time   datetime                      not null,
    update_time   datetime                      not null,
    deleted       bit         default b'0'      not null
)
    comment '秒杀活动' charset = utf8mb4;

create index idx_seckill_sku
    on mkt_seckill (sku_id);

create table mkt_user_coupon
(
    id            bigint auto_increment
        primary key,
    user_id       bigint                       not null,
    coupon_code   varchar(64)                  null comment '券码，全局唯一',
    template_id   bigint                       not null comment '来源模板 ID',
    status        varchar(20) default 'UNUSED' not null,
    order_no      varchar(32)                  null,
    received_time datetime                     not null comment '领取时间',
    expire_time   datetime                     not null,
    used_time     datetime                     null comment '核销时间，未使用时为空',
    create_time   datetime                     not null,
    update_time   datetime                     not null,
    deleted       bit         default b'0'     not null
)
    comment '用户优惠券' charset = utf8mb4;

create index idx_user_coupon_user
    on mkt_user_coupon (user_id, status);

create table oms_after_sale
(
    id                  bigint auto_increment
        primary key,
    after_sale_no       varchar(32)                   not null,
    order_id            bigint                        not null,
    order_no            varchar(32)                   not null,
    user_id             bigint                        not null,
    type                varchar(20)                   not null,
    reason              varchar(255)                  not null,
    refund_amount       decimal(12, 2)                not null comment '申请退款金额',
    status              varchar(20) default 'APPLIED' not null,
    apply_time          datetime                      null comment '申请时间，7 天无理由时效判断',
    origin_order_status varchar(20)                   null comment '申请前的订单状态，驳回时回滚用',
    handle_remark       varchar(255)                  null comment '处理备注，驳回时必须写理由',
    audit_by            varchar(50)                   null,
    handle_time         datetime                      null comment '处理时间',
    create_time         datetime                      not null,
    update_time         datetime                      not null,
    deleted             bit         default b'0'      not null
)
    comment '售后单' charset = utf8mb4;

create index idx_after_sale_order
    on oms_after_sale (order_id);

create table oms_cart_item
(
    id          bigint auto_increment
        primary key,
    user_id     bigint           not null,
    product_id  bigint           not null,
    sku_id      bigint           not null,
    quantity    int default 1    not null,
    checked     bit default b'1' not null comment '是否勾选结算：1 勾选 0 未勾选',
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null,
    constraint uk_cart_user_sku
        unique (user_id, sku_id)
)
    comment '购物车' charset = utf8mb4;

create index idx_cart_user
    on oms_cart_item (user_id);

create table oms_order
(
    id                bigint auto_increment
        primary key,
    order_no          varchar(32)                       not null,
    user_id           bigint                            not null,
    total_amount      decimal(12, 2) default 0.00       not null,
    discount_amount   decimal(12, 2) default 0.00       not null,
    coupon_amount     decimal(12, 2) default 0.00       not null,
    freight_amount    decimal(12, 2) default 0.00       not null,
    pay_amount        decimal(12, 2) default 0.00       not null,
    status            varchar(20)    default 'WAIT_PAY' not null,
    pay_type          varchar(20)                       null,
    pay_time          datetime                          null,
    expire_time       datetime                          not null,
    close_time        datetime                          null,
    receiver_snapshot text                              not null,
    remark            varchar(255)                      null,
    request_no        varchar(40)                       not null comment '幂等键',
    coupon_id         bigint                            null,
    seckill_id        bigint                            null,
    item_count        int            default 0          not null,
    logistics_company varchar(50)                       null,
    tracking_no       varchar(50)                       null,
    create_time       datetime                          not null,
    update_time       datetime                          not null,
    deleted           bit            default b'0'       not null,
    constraint uk_order_no
        unique (order_no),
    constraint uk_order_request_no
        unique (request_no)
)
    comment '订单' charset = utf8mb4;

create index idx_order_user_status_time
    on oms_order (user_id, status, create_time);

create table oms_order_item
(
    id            bigint auto_increment
        primary key,
    order_id      bigint           not null,
    order_no      varchar(32)      not null,
    product_id    bigint           not null,
    sku_id        bigint           not null,
    product_title varchar(128)     not null,
    sku_spec      varchar(100)     null,
    product_image varchar(255)     null comment '下单时商品主图快照',
    price         decimal(10, 2)   not null,
    quantity      int              not null,
    total_amount  decimal(12, 2)   not null,
    create_time   datetime         not null,
    update_time   datetime         not null,
    deleted       bit default b'0' not null
)
    comment '订单明细' charset = utf8mb4;

create index idx_order_item_order
    on oms_order_item (order_id);

create table oms_order_log
(
    id            bigint auto_increment
        primary key,
    order_id      bigint           not null,
    order_no      varchar(32)      not null,
    from_status   varchar(20)      null,
    to_status     varchar(20)      not null,
    operator_type varchar(50)      null comment '操作人类型：USER/ADMIN/SYSTEM',
    operator_id   bigint           null comment '操作人 ID，SYSTEM 时为空',
    remark        varchar(255)     null,
    create_time   datetime         not null,
    update_time   datetime         not null,
    deleted       bit default b'0' not null
)
    comment '订单状态流水' charset = utf8mb4;

create index idx_order_log_order
    on oms_order_log (order_id);

create table pay_payment
(
    id            bigint auto_increment
        primary key,
    payment_no    varchar(32)                   not null comment '支付单号',
    order_id      bigint                        null comment '订单 ID（实体未映射，改可空以免插入失败）',
    order_no      varchar(32)                   not null,
    user_id       bigint                        not null,
    amount        decimal(12, 2)                not null,
    channel       varchar(20) default 'MOCK'    not null,
    status        varchar(20) default 'PENDING' not null,
    trade_no      varchar(64)                   null,
    callback_time datetime                      null,
    create_time   datetime                      not null,
    update_time   datetime                      not null,
    deleted       bit         default b'0'      not null,
    constraint uk_pay_no
        unique (payment_no)
)
    comment '支付单' charset = utf8mb4;

create index idx_payment_order
    on pay_payment (order_no);

create table pms_brand
(
    id          bigint auto_increment
        primary key,
    name        varchar(50)      not null,
    logo        varchar(255)     null,
    description varchar(255)     null,
    sort        int default 0    not null,
    enabled     bit default b'1' not null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '品牌' charset = utf8mb4;

create table pms_category
(
    id          bigint auto_increment
        primary key,
    parent_id   bigint default 0    not null comment '父分类，0 表示一级',
    name        varchar(50)         not null,
    level       int    default 1    not null,
    sort        int    default 0    not null,
    icon        varchar(255)        null,
    enabled     bit    default b'1' not null,
    create_time datetime            not null,
    update_time datetime            not null,
    deleted     bit    default b'0' not null
)
    comment '商品分类' charset = utf8mb4;

create index idx_category_parent
    on pms_category (parent_id);

create table pms_favorite
(
    id          bigint auto_increment
        primary key,
    user_id     bigint           not null,
    product_id  bigint           not null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null,
    constraint uk_favorite_user_product
        unique (user_id, product_id)
)
    comment '商品收藏' charset = utf8mb4;

create index idx_favorite_user
    on pms_favorite (user_id);

create table pms_product
(
    id                bigint auto_increment
        primary key,
    category_id       bigint                             not null,
    brand_id          bigint                             null,
    title             varchar(128)                       not null,
    subtitle          varchar(255)                       null,
    main_image        varchar(255)                       null,
    images            varchar(1000)                      null comment '图集逗号分隔',
    detail            text                               null,
    ai_selling_points text                               null comment 'AI 卖点',
    price             decimal(10, 2) default 0.00        not null comment '最低 SKU 价',
    status            varchar(20)    default 'OFF_SHELF' not null,
    sales             int            default 0           not null,
    rating            decimal(3, 2)  default 5.00        not null,
    review_count      int            default 0           not null,
    create_time       datetime                           not null,
    update_time       datetime                           not null,
    deleted           bit            default b'0'        not null
)
    comment '商品 SPU' charset = utf8mb4;

create index idx_product_brand
    on pms_product (brand_id);

create index idx_product_category_status
    on pms_product (category_id, status);

create table pms_review
(
    id          bigint auto_increment
        primary key,
    product_id  bigint           not null,
    sku_id      bigint           null,
    order_id    bigint           not null,
    user_id     bigint           not null,
    rating      int              not null comment '1-5 星',
    content     text             not null,
    images      varchar(1000)    null,
    anonymous   bit default b'0' not null comment '是否匿名',
    reply       text             null,
    nickname    varchar(30)      null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '商品评价' charset = utf8mb4;

create index idx_review_order
    on pms_review (order_id);

create index idx_review_product
    on pms_review (product_id);

create table pms_sku
(
    id          bigint auto_increment
        primary key,
    product_id  bigint           not null,
    spec        varchar(100)     not null,
    price       decimal(10, 2)   not null,
    stock       int default 0    not null,
    image       varchar(255)     null,
    enabled     bit default b'1' not null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '商品 SKU' charset = utf8mb4;

create index idx_sku_product
    on pms_sku (product_id);

create table sys_audit_log
(
    id          bigint auto_increment
        primary key,
    user_id     bigint           null,
    username    varchar(50)      null,
    module      varchar(50)      null comment '业务模块，如 order / marketing',
    method      varchar(10)      not null comment 'HTTP 方法：GET/POST/PUT/DELETE',
    uri         varchar(255)     not null comment '请求路径',
    params      varchar(1000)    null comment '请求参数摘要（敏感字段已脱敏）',
    action      varchar(64)      null,
    ip          varchar(64)      null,
    success     bit default b'1' not null,
    status_code int              null,
    cost_ms     bigint           null comment '请求耗时（毫秒）',
    error_msg   varchar(500)     null comment '失败原因摘要，成功时为空',
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '操作审计日志' charset = utf8mb4;

create index idx_audit_user_time
    on sys_audit_log (user_id, create_time);

create table user_account
(
    id                bigint auto_increment comment '主键'
        primary key,
    username          varchar(50)                 not null comment '账号',
    password          varchar(100)                not null comment 'BCrypt 密码',
    phone             varchar(20)                 not null comment '手机号',
    nickname          varchar(50)                 null comment '昵称',
    avatar            varchar(255)                null comment '头像',
    role              varchar(20)                 not null comment '角色：CUSTOMER/SERVICE/OPERATOR/ADMIN',
    status            varchar(20)                 not null comment '状态：ENABLED/DISABLED',
    points            int            default 0    not null comment '积分',
    level             int            default 1    not null comment '会员等级',
    total_consumption decimal(12, 2) default 0.00 not null comment '累计消费',
    token_version     int            default 0    not null comment '令牌版本（登出失效）',
    last_login_time   datetime                    null comment '最后登录时间',
    last_login_ip     varchar(64)                 null comment '最后登录 IP',
    create_time       datetime                    not null comment '创建时间',
    update_time       datetime                    not null comment '更新时间',
    deleted           bit            default b'0' not null comment '逻辑删除',
    constraint uk_user_phone
        unique (phone),
    constraint uk_user_username
        unique (username)
)
    comment '用户账号' charset = utf8mb4;

create table user_address
(
    id          bigint auto_increment
        primary key,
    user_id     bigint           not null comment '所属用户',
    receiver    varchar(30)      not null comment '收货人',
    phone       varchar(20)      not null comment '联系电话',
    province    varchar(30)      not null,
    city        varchar(30)      not null,
    district    varchar(30)      not null,
    detail      varchar(200)     not null,
    is_default  bit default b'0' not null,
    tag         varchar(20)      null,
    create_time datetime         not null,
    update_time datetime         not null,
    deleted     bit default b'0' not null
)
    comment '收货地址' charset = utf8mb4;

create index idx_address_user
    on user_address (user_id);

