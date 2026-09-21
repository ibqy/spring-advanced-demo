package com.ibqy.spring.advanced.demo05_observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ============================================================
 * Demo 05: 可观测性 (Observability) 演示
 * ============================================================
 *
 * 【背景知识】
 * 可观测性 (Observability) 是现代分布式系统的核心能力，包含三大支柱：
 * 1. Metrics（指标）：数值型时序数据（QPS、延迟、错误率）
 * 2. Logs（日志）：离散事件记录
 * 3. Traces（链路追踪）：请求在各服务间的传播路径
 *
 * Spring Boot 4.1 的可观测性栈：
 * - Micrometer：指标采集门面（类似 SLF4J 之于日志）
 * - Micrometer Observation API：统一的观测 API（指标 + 追踪）
 * - Spring Boot Actuator：暴露指标端点
 * - Prometheus/Grafana：指标存储和可视化
 *
 * 【核心组件】
 * - ObservationRegistry：观测注册中心（创建和管理 Observation）
 * - MeterRegistry：指标注册中心（Micrometer 核心接口）
 * - ObservationConvention：标准化指标命名
 * - @Timed / @Counted：声明式指标注解
 *
 * 【面试考点】
 * 1. Micrometer vs Dropwizard Metrics：Micrometer 是 Spring 官方门面
 * 2. Observation vs Meter：Observation 包含追踪信息，Meter 只有指标
 * 3. 指标类型：Counter（累加）/ Timer（计时）/ Gauge（瞬时值）/ Summary（分位数）
 * 4. 标签(Tags)的作用：同一指标按维度拆分（如按 HTTP 状态码分组）
 * 5. Prometheus 数据模型：metric_name{labels} value timestamp
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
public class ObservabilityDemo {

    /**
     * Micrometer 的指标注册中心
     * 所有自定义指标都通过它注册
     */
    private final MeterRegistry meterRegistry;

    /**
     * Micrometer Observation API 的注册中心
     * 统一的观测入口，同时支持指标和追踪
     */
    private final ObservationRegistry observationRegistry;

    /**
     * 显式构造函数注入（教学规范：不使用 Lombok）
     */
    public ObservabilityDemo(MeterRegistry meterRegistry,
                             ObservationRegistry observationRegistry) {
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
    }

    /**
     * 应用启动完成后运行演示
     */
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        System.out.println("""
                
                ====================================================
                 Demo 05: 可观测性 (Observability) 演示
                ====================================================
                """);

        demoMeterRegistry();
        demoObservationApi();
        demoObservationConvention();
        demoPrometheusOutput();
        demoInterviewTips();

        System.out.println("[Demo 05 完成] 可观测性演示结束");
        System.out.println("====================================================");
    }

    /**
     * 演示 1: MeterRegistry 自定义指标
     *
     * 【Micrometer 指标类型】
     * - Counter：只增不减的计数器（如请求总数、错误总数）
     * - Timer：带计时的计数器（如请求延迟、方法执行时间）
     * - Gauge：瞬时值仪表（如内存使用量、队列长度）
     * - DistributionSummary：分布摘要（如响应体大小、批次大小）
     */
    private void demoMeterRegistry() {
        System.out.println("[1] MeterRegistry 自定义指标");
        System.out.println("--------------------------------------------");

        // =============================================
        // 1.1 Counter - 计数器
        // =============================================
        System.out.println("\n  [1.1] Counter (计数器) - 记录事件发生次数");

        // 创建计数器：记录 API 请求总数
        Counter apiCounter = Counter.builder("demo.api.requests.total")
                .description("API 请求总数")
                .tag("service", "demo05")
                .tag("version", "1.0")
                .register(meterRegistry);

        // 模拟 100 次 API 请求
        for (int i = 0; i < 100; i++) {
            apiCounter.increment();
        }
        System.out.println("  API 请求计数器: " + apiCounter.count() + " (已记录 100 次请求)");

        // 带标签的计数器：按状态码分组
        Counter successCounter = Counter.builder("demo.api.requests.success")
                .description("成功请求数")
                .tag("status", "200")
                .tag("method", "GET")
                .register(meterRegistry);

        Counter errorCounter = Counter.builder("demo.api.requests.error")
                .description("错误请求数")
                .tag("status", "500")
                .tag("method", "GET")
                .register(meterRegistry);

        // 模拟 80 次成功 + 20 次失败
        for (int i = 0; i < 80; i++) {
            successCounter.increment();
        }
        for (int i = 0; i < 20; i++) {
            errorCounter.increment();
        }
        System.out.println("  成功请求: " + successCounter.count() + ", 失败请求: " + errorCounter.count());

        // =============================================
        // 1.2 Timer - 计时器
        // =============================================
        System.out.println("\n  [1.2] Timer (计时器) - 记录操作耗时");

        Timer dbTimer = Timer.builder("demo.db.query.duration")
                .description("数据库查询耗时")
                .tag("operation", "select")
                .tag("table", "users")
                .publishPercentiles(0.5, 0.95, 0.99)  // 发布分位数
                .register(meterRegistry);

        // 模拟 50 次数据库查询
        for (int i = 0; i < 50; i++) {
            // Timer.record() 会记录执行时间
            dbTimer.record(() -> {
                try {
                    // 模拟数据库查询耗时（1-50ms）
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1, 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        System.out.println("  数据库查询统计:");
        System.out.println("    总次数: " + dbTimer.count());
        System.out.println("    总耗时: " + dbTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) + "ms");
        System.out.println("    平均耗时: " + dbTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS) + "ms");
        System.out.println("    最大耗时: " + dbTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS) + "ms");

        // =============================================
        // 1.3 Gauge - 仪表盘
        // =============================================
        System.out.println("\n  [1.3] Gauge (仪表盘) - 记录瞬时值");

        // Gauge 与其他指标不同，它读取的是当前值（而非累加）
        // 通过 lambda 表达式动态获取当前值
        final int[] queueSize = {42}; // 模拟队列大小

        io.micrometer.core.instrument.Gauge.builder("demo.queue.size", () -> queueSize[0])
                .description("消息队列当前大小")
                .tag("queue", "orders")
                .register(meterRegistry);

        System.out.println("  当前队列大小: " + queueSize[0]);

        // 模拟队列变化
        queueSize[0] = 100;
        System.out.println("  队列变化后: " + queueSize[0] + " (Gauge 自动反映最新值)");

        // =============================================
        // 1.4 指标类型对比总结
        // =============================================
        System.out.println("""
                
                  [指标类型对比]
                  +----------+-------------+-----------+------------+------------------+
                  | 类型     | 特点        | 典型场景   | 是否累加    | Prometheus 映射   |
                  +----------+-------------+-----------+------------+------------------+
                  | Counter  | 只增不减     | 请求总数   | 是         | counter          |
                  | Timer    | 计数+计时    | 方法耗时   | 是         | summary(histogram)|
                  | Gauge    | 瞬时值      | 队列长度   | 否         | gauge            |
                  | Summary  | 分布统计     | 响应体大小 | 是         | summary          |
                  +----------+-------------+-----------+-----------+------------------+
                """);
    }

    /**
     * 演示 2: Observation API
     *
     * Observation 是 Micrometer 的统一观测 API，
     * 同时支持 Metrics（指标）和 Tracing（追踪）。
     *
     * 【为什么需要 Observation？】
     * - 之前：Metrics 和 Tracing 是两套独立的 API
     * - 现在：Observation 统一入口，同时产生指标和追踪数据
     * - 好处：指标和追踪自动关联，共享上下文（如 traceId）
     */
    private void demoObservationApi() {
        System.out.println("""
                
                [2] Observation API - 统一观测接口
                --------------------------------------------""");

        // =============================================
        // 2.1 创建简单的 Observation
        // =============================================
        System.out.println("\n  [2.1] 创建简单 Observation");

        // Observation 的创建和使用
        Observation observation = Observation.createNotStarted(
                "demo.order.processing",  // 观测名称
                observationRegistry
        );

        // 添加上下文信息（会同时传递给 Metrics 和 Tracing）
        observation.lowCardinalityKeyValue("order.type", "standard");  // 低基数标签（适合 Metrics）
        observation.lowCardinalityKeyValue("order.source", "web");     // 低基数 -> 用作 Prometheus 标签
        observation.highCardinalityKeyValue("order.id", "ORD-20260921-001");  // 高基数 -> 仅 Tracing
        observation.highCardinalityKeyValue("order.customer", "张三");          // 高基数 -> 不适合作为 Metrics 标签

        // 启动观测
        observation.start();
        System.out.println("  Observation 已启动: demo.order.processing");
        System.out.println("  低基数标签: order.type=standard, order.source=web");
        System.out.println("  高基数标签: order.id=ORD-20260921-001, order.customer=张三");

        try {
            // 模拟业务处理
            Thread.sleep(150);
            observation.event(Observation.Event.of("order.validated", "订单验证通过"));
            Thread.sleep(100);
            observation.event(Observation.Event.of("order.processed", "订单处理完成"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            observation.error(e);
        } finally {
            observation.stop();
            System.out.println("  Observation 已停止（总耗时约 250ms）");
        }

        // =============================================
        // 2.2 使用 Observation.scope() 简化代码
        // =============================================
        System.out.println("\n  [2.2] 使用 observe() 简化写法");

        // 更简洁的写法：observe() 自动处理 start/stop/error
        String result = Observation.createNotStarted("demo.user.lookup", observationRegistry)
                .lowCardinalityKeyValue("lookup.type", "by-email")
                .observe(() -> {
                    // 这个 lambda 的执行时间会被自动记录
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "user@example.com";
                });

        System.out.println("  查询结果: " + result);
        System.out.println("  自动记录了执行时间（约 50ms）");

        // =============================================
        // 2.3 带 Context 的 Observation
        // =============================================
        System.out.println("\n  [2.3] 带 Context 的 Observation");

        // Observation.Context 用于在观测过程中传递上下文信息
        // 类似 MDC（Mapped Diagnostic Context）但专为可观测性设计
        OrderContext context = new OrderContext("ORD-001", "VIP");

        Observation.createNotStarted("demo.order.with-context", observationRegistry)
                .contextualName("order-processing-ORD-001")
                .start()
                .stop();

        System.out.println("  使用 Context 传递订单信息: orderId=" + context.orderId()
                + ", customerType=" + context.customerType());

        // =============================================
        // 2.4 Observation 工作原理
        // =============================================
        System.out.println("""
                
                  [Observation 工作原理]
                  
                  1. 创建阶段:
                     Observation.createNotStarted(name, registry)
                     └── 查找匹配的 ObservationConvention
                     └── 确定指标名称和标签
                  
                  2. 启动阶段:
                     observation.start()
                     └── 启动 Timer（指标）
                     └── 创建 Span（追踪）
                     └── 将上下文压入 ThreadLocal
                  
                  3. 运行阶段:
                     observation.observe(() -> { ... })
                     └── 执行业务逻辑
                     └── 自动处理异常 (observation.error())
                  
                  4. 停止阶段:
                     observation.stop()
                     └── 停止 Timer（记录指标）
                     └── 关闭 Span（发送追踪数据）
                     └── 清理 ThreadLocal
                  
                  5. 输出:
                     Metrics -> Micrometer -> Prometheus/InfluxDB
                     Traces  -> Brave/OTel  -> Zipkin/Jaeger
                """);
    }

    /**
     * 演示 3: 自定义 ObservationConvention
     *
     * ObservationConvention 的作用：
     * 1. 标准化指标命名（所有服务使用统一的命名规范）
     * 2. 集中管理标签（避免各服务标签不一致）
     * 3. 支持低基数/高基数标签分离
     */
    private void demoObservationConvention() {
        System.out.println("""
                
                [3] 自定义 ObservationConvention
                --------------------------------------------
                
                === 为什么需要 ObservationConvention？ ===
                
                1. 命名一致性：
                   - 没有 Convention：每个开发者自己定义指标名
                   - 有 Convention：团队统一的命名规范
                
                2. 标签管理：
                   - 没有 Convention：标签散落在各处
                   - 有 Convention：集中定义、统一管理
                
                3. 基数控制：
                   - Convention 明确区分 lowCardinality (标签) 和 highCardinality (仅追踪)
                   - 防止误用高基数值作为 Prometheus 标签（导致指标爆炸）
                
                === 代码示例 ===
                \s""" + """
                // 1. 定义 Context（携带业务上下文信息）
                public class OrderContext extends Observation.Context {
                    private final String orderId;
                    private final String customerType;
                    
                    public OrderContext(String orderId, String customerType) {
                        this.orderId = orderId;
                        this.customerType = customerType;
                    }
                    // getters...
                }
                
                // 2. 定义 Convention（标准化命名和标签）
                public class OrderObservationConvention 
                        implements ObservationConvention<OrderContext> {
                    
                    @Override
                    public boolean supportsContext(Observation.Context context) {
                        return context instanceof OrderContext;
                    }
                    
                    @Override
                    public String getName() {
                        return "orders.processing";  // 统一指标名
                    }
                    
                    @Override
                    public KeyValueValues getLowCardinalityKeyValues(OrderContext context) {
                        // 低基数标签 -> 用作 Prometheus 标签
                        return KeyValueValues.of(
                            "order.type", context.getCustomerType(),  // VIP / STANDARD
                            "step", "processing"
                        );
                    }
                    
                    @Override
                    public KeyValueValues getHighCardinalityKeyValues(OrderContext context) {
                        // 高基数标签 -> 仅用于追踪，不影响指标基数
                        return KeyValueValues.of(
                            "order.id", context.getOrderId()  // 每个订单不同
                        );
                    }
                }
                
                // 3. 注册 Convention
                @Bean
                public OrderObservationConvention orderConvention() {
                    return new OrderObservationConvention();
                }
                """);

        System.out.println("""
                  [Convention vs 直接创建]
                  
                  // 直接创建（不推荐）:
                  Observation.createNotStarted("order.processing", registry)
                      .lowCardinalityKeyValue("type", "VIP")
                      .lowCardinalityKeyValue("step", "processing")
                      .highCardinalityKeyValue("orderId", "ORD-001")
                      .start()
                      .stop();
                  
                  // 使用 Convention（推荐）:
                  OrderContext ctx = new OrderContext("ORD-001", "VIP");
                  Observation.start(orderConvention, ctx, registry).stop();
                  
                  // 优势：
                  // 1. 命名和标签由 Convention 统一管理
                  // 2. 业务代码只需关注 Context 数据
                  // 3. 更换命名规范只改 Convention，不改业务代码
                  // 4. 方便做 A/B 测试（切换不同的 Convention）
                """);
    }

    /**
     * 演示 4: Prometheus 端点输出展示
     */
    private void demoPrometheusOutput() {
        System.out.println("""
                
                [4] Prometheus 端点输出展示
                --------------------------------------------
                
                === 访问端点 ===
                curl http://localhost:8080/actuator/prometheus
                
                === 输出格式示例 ===
                
                # HELP demo_api_requests_total API 请求总数
                # TYPE demo_api_requests_total counter
                demo_api_requests_total{service="demo05",version="1.0",} 100.0
                demo_api_requests_success{method="GET",status="200",} 80.0
                demo_api_requests_error{method="GET",status="500",} 20.0
                
                # HELP demo_db_query_duration_seconds 数据库查询耗时
                # TYPE demo_db_query_duration_seconds summary
                demo_db_query_duration_seconds{operation="select",table="users",quantile="0.5",} 0.025
                demo_db_query_duration_seconds{operation="select",table="users",quantile="0.95",} 0.048
                demo_db_query_duration_seconds{operation="select",table="users",quantile="0.99",} 0.050
                demo_db_query_duration_seconds_count{operation="select",table="users",} 50.0
                demo_db_query_duration_seconds_sum{operation="select",table="users",} 1.250
                
                # HELP demo_queue_size 消息队列当前大小
                # TYPE demo_queue_size gauge
                demo_queue_size{queue="orders",} 100.0
                
                === 在 Grafana 中使用 ===
                1. 添加 Prometheus 数据源 -> http://prometheus:9090
                2. 创建 Panel，使用 PromQL 查询:
                   - 请求速率: rate(demo_api_requests_total[5m])
                   - P99 延迟: histogram_quantile(0.99, rate(demo_db_query_duration_seconds_bucket[5m]))
                   - 错误率: rate(demo_api_requests_error[5m]) / rate(demo_api_requests_total[5m])
                
                === 相关 Actuator 端点 ===
                - /actuator/metrics          -> 所有指标列表
                - /actuator/metrics/{name}   -> 指定指标的详情
                - /actuator/prometheus       -> Prometheus 格式输出
                - /actuator/health           -> 健康检查
                - /actuator/info             -> 应用信息
                """);
    }

    /**
     * 演示 5: 面试考点总结
     */
    private void demoInterviewTips() {
        System.out.println("""
                [5] 面试考点总结
                --------------------------------------------
                
                Q: Micrometer 和 Spring Boot Actuator 的关系？
                A: - Micrometer 是指标采集的门面 API（类似 SLF4J）
                   - Actuator 是 Spring Boot 的运维端点（暴露指标、健康检查等）
                   - Actuator 底层使用 Micrometer 采集指标
                   - Actuator 提供 /actuator/prometheus 端点输出指标
                
                Q: @Timed 注解的原理？
                A: 1. Spring AOP 拦截 @Timed 标注的方法
                   2. 创建 Observation（包含 Timer）
                   3. 记录方法执行时间
                   4. 自动添加标签（类名、方法名、异常类型）
                   5. 支持 percentiles 配置（P50/P95/P99）
                
                Q: 低基数 vs 高基数标签的区别？
                A: - 低基数 (Low Cardinality)：标签值数量有限
                    例：status=200, method=GET -> 适合 Prometheus 标签
                  - 高基数 (High Cardinality)：标签值几乎无限
                    例：userId=12345, orderId=ORD-001 -> 只能用于追踪
                  - 误用高基数标签的后果：
                    Prometheus 时间序列爆炸 -> 内存暴涨 -> OOM
                
                Q: 如何设计一个好的指标体系？
                A: 1. RED 方法（Rate, Errors, Duration）
                   - Rate：每秒请求数
                   - Errors：每秒错误数
                   - Duration：每个请求的耗时分布
                   2. USE 方法（Utilization, Saturation, Errors）
                   - 适合资源级别监控（CPU、内存、磁盘）
                   3. 四个黄金信号（Google SRE）
                   - Latency / Traffic / Errors / Saturation
                
                Q: Observation 和 Meter 的区别？
                A: - Meter：只有指标数据（Counter/Timer/Gauge）
                   - Observation：指标 + 追踪 + 上下文
                   - Observation 是 Meter 的超集
                   - 新项目推荐直接使用 Observation API
                   - 旧项目的 Meter 代码可以继续用（兼容）
                
                Q: 如何实现分布式链路追踪？
                A: 1. 引入 Micrometer Tracing（原 Spring Cloud Sleuth）
                   2. 配置 Brave 或 OpenTelemetry 作为 Tracer
                   3. Observation 自动传播 traceId（通过 HTTP Header / MQ Message）
                   4. 使用 Zipkin 或 Jaeger 收集和展示链路
                   5. Spring Boot 4.1 自动配置 ObservationTextPublisher
                """);
    }

    /**
     * 内部记录类：订单上下文
     * 用于演示 Observation.Context 的用法
     *
     * @param orderId      订单 ID
     * @param customerType 客户类型 (VIP/STANDARD)
     */
    record OrderContext(String orderId, String customerType) {
    }
}
