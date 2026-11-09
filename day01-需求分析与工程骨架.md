# Day 01 · 需求分析、架构设计与工程骨架


**当日产出**：一个能启动、能打开 Swagger、返回结构统一的 Spring Boot 工程
**验收口令**：`mvn spring-boot:run` 启动成功 → 访问 `http://localhost:8080/swagger-ui.html`

---

## 0. 先看全局：10 天怎么分

| 天 | 主题 | 对应章节 | 当日产出 | 文件数 | 代码量（约） |
| --- | --- | --- | --- | --- | --- |
| 1 | 需求分析、架构设计、工程骨架 | 12 / 14 / 15 / 17.1 | 可启动工程、统一返回、全局异常 | 9 | 520 行 |
| 2 | 数据库建模与实体层 | 16 全章 | 24 张表设计、BaseEntity、核心实体与仓储、演示数据 | 12 | 700 行 |
| 3 | 认证授权与用户中心 | 13.2.1 / 15.9 / 17.2 | JWT 双令牌、RBAC、限流、注册登录、地址、积分 | 14 | 900 行 |
| 4 | 商品中心 | 13.2.2 / 16.3 / 17.3 | 分类树、SPU/SKU、检索、缓存、条件扣库存 | 11 | 850 行 |
| 5 | 购物车、评价与收藏 | 13.2.2 / 17.4 / 17.9 | 加购合并、勾选结算、评分聚合、敏感词 | 9 | 620 行 |
| 6 | 计价、下单、状态机 | 13.3 / 17.5 / 17.13 / 17.15 | 优惠计价、幂等下单、防超卖、状态机 | 12 | 980 行 |
| 7 | 支付、关单、售后、秒杀 | 13.2.3 / 17.5.2 / 19.7 / 23.7 | 支付回调验签幂等、超时关单、售后、秒杀 | 10 | 780 行 |
| 8 | Spring AI 与 RAG 基础 | 15.6 / 22.1 / 22.3 / 22.6 / 22.11 | 向量化、切片、索引、检索、知识库入库 | 10 | 860 行 |
| 9 | AI 客服、Function Call、推荐 | 22.2 / 22.7 / 22.8 / 22.9 / 22.12 | SSE 流式问答、工具调用、多轮、转人工 | 9 | 960 行 |
| 10 | 看板、审计、测试、部署与答辩 | 23 / 24 / 25 / 26 / 27 | 经营看板、审计日志、单测、容器化、答辩 | 12 | 700 行 |

> **两种使用方式**
>
> - **A. 拆解阅读（推荐）**：仓库里是完整可运行代码，每天读对应模块 + 按"自测清单"验证。
> - **B. 从零跟做**：第 1~2 天先把 `DataSeeder` 与 AI 相关 Bean 注释掉（它们依赖后面几天才写的类），第 8 天再恢复。

---

## 1. 需求分析：把一句话需求拆成八块

文档 13.2 把系统分成八大模块。我们做的是**后端**，落到可编码的粒度是这样：

```
用户中心  注册/登录/令牌/角色/地址/积分等级
商品中心  分类树/品牌/SPU-SKU/检索/上下架/库存
交易中心  购物车/订单/状态机/超时关单/售后
营销中心  优惠券/满减/秒杀/统一计价
智能中心  AI 客服(RAG)/Function Call/推荐/知识库
运营后台  商品/订单/营销/知识库管理 + 经营看板
基础设施  统一返回/异常/JWT/RBAC/限流/审计/traceId
演进预留  缓存、消息、锁、微服务的替换点
```

**三个必须落到代码的业务规则**（文档 13.9）：

1. 价格以**下单时快照**为准 → 订单明细存商品名/规格/单价，订单存收货信息 JSON。
2. 优惠不可叠加时**取最优** → 计价器算完满减与券后二选一。
3. 订单**严格状态机**流转 → 非法跳转直接抛 13002，绝不允许"已取消被发货"。

**非功能需求**对应到本项目的技术手段：

| 非功能 | 手段 |
| --- | --- |
| 防超卖 | `UPDATE ... WHERE stock >= ?` 条件更新 |
| 幂等 | 订单 `request_no` 唯一索引 + 支付单状态幂等 |
| 防刷 | 登录（IP）、下单、秒杀、AI 问答（用户）滑动窗口限流 |
| 可观测 | 每请求 traceId（MDC + 响应头）+ 写操作审计日志 |
| 安全 | BCrypt 密码、JWT 版本号登出、越权校验、敏感词过滤 |

---

## 2. 技术选型

| 选择                       | 理由                                                       |
| ------------------------ | -------------------------------------------------------- |
| **Java 21**              | Spring AI 1.0 的硬性要求；record / var / 文本块 / 模式匹配都能用         |
| **Spring Boot 3.4.5**    | Spring AI 1.0.0 GA 对应的 Boot 3.4.x 系列                     |
| **Spring AI 1.0.0**      | 统一 ChatClient / EmbeddingModel / Tool Calling，换模型只改配置    |
| mybatis-plus             | 24 张表用实体 + Mapper 接口表达，避免大量 XML；复杂查询用 LambdaQueryWrapper |
| **H2（默认）/ MySQL 8**      | H2 免安装先跑通，`mysql` Profile 切生产库，同一套代码                     |
| **Caffeine**             | 单体阶段热点缓存，接口不变，后续换 Redis                                  |
| **jjwt 0.12.6 + BCrypt** | 无状态认证；只用 spring-security-crypto，不引入整条过滤器链                |

---

## 3. 工程骨架

### 3.1 目录结构（先立规矩，再写代码）

```
smart-mall
├── pom.xml
├── src/main/java/com/smartmall
│   ├── SmartMallApplication.java        ← 启动类
│   ├── common/                           ← 基础设施（所有人共用）
│   │   ├── api/         Result、PageResult、ResultCode
│   │   ├── exception/   BizException、GlobalExceptionHandler
│   │   ├── security/    JWT、UserContext、RBAC 注解
│   │   ├── ratelimit/   限流注解与滑动窗口实现
│   │   ├── persistence/ BaseEntity（审计字段 + 逻辑删除）
│   │   └── config/      各类 @ConfigurationProperties
│   ├── user/  catalog/  cart/  order/  pay/  marketing/  review/
│   ├── ai/                              ← Spring AI 相关
│   ├── stats/   audit/   bootstrap/     ← 看板、审计、演示数据
└── src/main/resources/application.yml
```

**包名即边界**：后续拆微服务时，`user` / `catalog` / `order` / `marketing` / `ai`
这五个包可以整体搬成独立模块，这也是文档 14.3 的拆分思路。

### 3.2 pom.xml（关键片段）

```xml
<properties>
    <java.version>21</java.version>
    <mybatis-plus.version>3.5.9</mybatis-plus.version>
    <spring-ai.version>1.0.0</spring-ai.version>
</properties>

<!-- 用 BOM 统一管理 Spring AI 全部依赖版本，子依赖不用写版本号 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- 只要 BCrypt，不引入完整 Spring Security 过滤器链 -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
    </dependency>

    <!-- Spring AI：统一大模型调用 / Embedding / Tool Calling -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <!-- Spring AI 流式调用依赖 Reactor；应用仍是 Servlet(MVC) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

**讲解**

- `spring-ai-bom` 用 `import` 方式引入，后面所有 `spring-ai-*` 都不用写版本，避免版本漂移。
- `spring-boot-starter-webflux` 只是为了提供 Reactor 与 `WebClient`；因为类路径同时存在 MVC 与 WebFlux，
  Boot 会判定为 **SERVLET** 应用，不会把项目变成响应式。
- 不引入 `spring-boot-starter-security` 是刻意的：我们自己做 JWT 过滤器 + 注解式 RBAC，
  代码量小、链路清晰，符合文档 17.2 "拦截器统一解析"的教学路径。

### 3.3 启动类

`src/main/java/com/smartmall/SmartMallApplication.java`

```java
@SpringBootApplication          // 组合注解：配置类 + 自动装配 + 组件扫描
@ConfigurationPropertiesScan    // 扫描 @ConfigurationProperties 的 record（本项目所有配置类）
@EnableCaching                  // 开启 @Cacheable（商品详情、分类树）
@EnableAsync                    // 开启 @Async（知识库向量化、审计日志）
@EnableScheduling               // 开启 @Scheduled（超时关单、优惠券过期）
public class SmartMallApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartMallApplication.class, args);
    }
}
```

> 四个 `@Enable*` 就是本项目的"时间维度"能力：缓存（快）、异步（不阻塞）、调度（定时）、配置（可切）。

### 3.4 application.yml（H2 默认 + MySQL Profile + AI 开关）

```yaml
spring:
  datasource:
    # 默认 H2 内存库：免安装，启动即用；MODE=MySQL 兼容 MySQL 语法
    url: jdbc:h2:mem:smartmall;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:sk-local-placeholder}   # 占位 Key，保证 Bean 能创建
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL:gpt-4o-mini}
          temperature: 0.3

app:
  jwt:
    secret: ${JWT_SECRET:smart-mall-jwt-secret-key-must-be-at-least-32-bytes-long-2025}
    access-ttl-seconds: 7200        # 访问令牌 2 小时
    refresh-ttl-seconds: 604800     # 刷新令牌 7 天
  order:
    pay-timeout-minutes: 30         # 超时关单时间
    free-freight-threshold: 99.00   # 满 99 包邮
    freight: 10.00
  ai:
    chat-provider: mock             # mock：离线规则引擎；openai：走 Spring AI
    embedding-provider: local       # local：哈希向量；openai：云端 Embedding

---
# MySQL 环境：--spring.profiles.active=mysql
spring:
  config:
    activate:
      on-profile: mysql
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:3306/smart_mall?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**讲解**

- MyBatis-Plus 默认不开会话懒加载，不存在 JPA 的 OSIV（Open Session In View）问题，
  实体转 VO 的动作天然放在 Service 层完成，事务边界清晰。
- 配置用 `${ENV:默认值}`，本地零配置、线上用环境变量，符合文档 15.11 的"环境差异用 Profile/环境变量"。
- `app.ai.chat-provider=mock` 是本项目的关键设计：**没有大模型 Key 也能演示完整 RAG 链路**，见 Day 8/9。

---

## 4. 统一返回：为什么用 record

`src/main/java/com/smartmall/common/api/Result.java`

```java
/**
 * 统一返回结构：code / message / data（文档 14.4 / 17.14 / 23.2）。
 *
 * @param code    业务码，2000 表示成功
 * @param message 提示信息
 * @param data    业务数据
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static Result<Void> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean successful() {
        return code == ResultCode.SUCCESS.getCode();
    }
}
```

**三句话讲清设计**

1. `record` 自带 `构造器 + getter + equals/hashCode/toString`，比手写 POJO 少 40 行，
   而且 Jackson 原生支持 record 序列化，不需要额外配置。
2. `code=2000` 是业务成功码，与 HTTP 200 解耦：前端只看 `code`，HTTP 状态码保持语义化。
3. 泛型 `<T>` 让 `Result<OrderVO>`、`Result<List<CartVO.Item>>` 都有编译期类型安全。

### 4.1 业务码分段

`src/main/java/com/smartmall/common/api/ResultCode.java`（节选）

```java
@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(2000, "成功"),

    /* ===== 通用 / 认证 10000-10999 ===== */
    PARAM_INVALID(4001, "参数校验失败"),
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    FORBIDDEN(4030, "无权限访问"),
    RATE_LIMITED(4029, "请求过于频繁，请稍后再试"),

    /* ===== 用户 11000-11999 ===== */
    USER_EXISTS(11001, "账号或手机号已被注册"),
    PASSWORD_ERROR(11003, "账号或密码错误"),

    /* ===== 订单 13000-13999 ===== */
    STOCK_NOT_ENOUGH(13001, "库存不足"),
    ORDER_STATUS_ILLEGAL(13002, "当前订单状态不允许该操作"),

    /* ===== AI 15000-15999 ===== */
    AI_UNAVAILABLE(15001, "智能助手暂不可用，请稍后再试或转人工"),
    CONTENT_REJECTED(15005, "内容不符合平台规范，已被拦截");

    private final int code;
    private final String message;
}
```

> 错误码分段的收益：前端可以按 `code / 1000` 判断归属模块，做差异化提示；
> 排查问题时看码就知道是哪个域抛的，文档 23.2 的要求。

---

## 5. 全局异常：把异常挡在 Controller 之外

### 5.1 业务异常

```java
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);            // 允许覆盖默认文案，例如「库存不足，当前仅剩 3 件」
        this.code = resultCode.getCode();
    }
}
```

### 5.2 统一处理器

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：预期内的失败，打 warn，不打堆栈 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：把 @NotBlank 等注解的 message 拼起来返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        var message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.PARAM_INVALID, message);
    }

    /** 兜底：系统异常绝不把堆栈暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);          // 完整堆栈只进日志
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}
```

**三个要点**

- `@RestControllerAdvice` 对所有 Controller 生效，且能捕获**拦截器 preHandle 里抛的异常**，
  所以 Day 3 的 `AccessInterceptor` 直接 `throw new BizException(UNAUTHORIZED)` 即可。
- 参数校验异常单独处理，因为它的 `message` 才是给用户看的（如"手机号格式不正确"）。
- 兜底 `Exception` 必须打完整堆栈，但返回给前端的只有"系统繁忙"，防止泄露内部信息。

### 5.3 分页封装

```java
public record PageResult<T>(List<T> records, long total, long pageNum, long pageSize, long pages) {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 实体分页 → VO 分页，避免把数据库实体直接暴露给前端 */
    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        return new PageResult<>(page.getRecords().stream().map(mapper).toList(),
                page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }
}
```

> MyBatis-Plus 的 `IPage` 页码从 1 开始，对外接口同样从 1 开始，语义一致；
> 分页插件（`PaginationInnerInterceptor`）会自动拼 `limit` 并回填 `total`。

---

## 6. 今日自测

```bash
# 1. 启动（首次会下载依赖，耐心等）
mvn spring-boot:run

# 2. 健康检查
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# 3. 接口文档
# 浏览器打开 http://localhost:8080/swagger-ui.html

# 4. 统一返回长什么样：随便访问一个不存在的接口
curl http://localhost:8080/api/not-exists
# {"code":4040,"message":"资源不存在"}
```

**今天最容易踩的坑**

| 现象                                     | 原因       | 处理                                             |
| -------------------------------------- | -------- | ---------------------------------------------- |
| 启动报 `Failed to configure a DataSource` | 没加 H2 依赖 | 确认 `com.h2database:h2` 是 runtime               |
| Swagger 404                            | 路径不对     | springdoc 默认 `/swagger-ui.html`，不是 `/doc.html` |
| 中文返回乱码                                 | 未指定编码    | yml 里 `server.servlet.encoding.force: true`    |

