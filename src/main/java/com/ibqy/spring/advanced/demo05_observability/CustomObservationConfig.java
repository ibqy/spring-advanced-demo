package com.ibqy.spring.advanced.demo05_observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================
 * Demo 05 配置: 自定义可观测性配置
 * ============================================================
 *
 * 【核心职责】
 * 展示如何配置自定义的 ObservationConvention，
 * 实现指标命名的标准化和标签管理的集中化。
 *
 * 【ObservationConvention 的设计哲学】
 * 1. 关注点分离：业务代码只关心业务逻辑，指标命名由 Convention 管理
 * 2. 单一职责：每个 Convention 负责一类观测指标
 * 3. 可替换性：可以在不修改业务代码的情况下更换指标策略
 * 4. 可测试性：Convention 是纯函数，容易单元测试
 *
 * 【面试考点】
 * 1. ObservationConvention 的查找顺序：
 *    a. ObservationRegistry 中注册的 Convention（按 supportsContext 匹配）
 *    b. Observation.createNotStarted() 传入的自定义 Convention
 *    c. 默认的 DefaultObservationConvention
 * 2. KeyValues vs KeyValue：KeyValues 是不可变的集合
 * 3. getName() 返回的指标名会作为 Prometheus 的 metric_name
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Configuration
public class CustomObservationConfig {

    /**
     * 注册订单处理观测的 Convention
     *
     * 当 Observation 的 Context 类型匹配时，自动使用此 Convention
     * 来决定指标名称和标签。
     *
     * 【使用示例】
     * Observation.createNotStarted("demo.order.processing", registry)
     *     .contextualName("process-order")
     *     .lowCardinalityKeyValue("type", "VIP")
     *     .start().stop();
     *
     * 有了 Convention 后可以简化为：
     * OrderContext ctx = new OrderContext("ORD-001", "VIP");
     * Observation.start(null, ctx, registry).stop();
     * // Convention 会自动提供名称和标签
     */
    @Bean
    public OrderProcessingObservationConvention orderProcessingConvention() {
        return new OrderProcessingObservationConvention();
    }

    /**
     * 注册 HTTP 请求观测的 Convention
     * 演示另一个 Convention 实现
     */
    @Bean
    public HttpRequestObservationConvention httpRequestConvention() {
        return new HttpRequestObservationConvention();
    }

    /**
     * =============================================
     * 订单处理 ObservationConvention 实现
     * =============================================
     *
     * 【命名规范】
     * - 使用点号分隔：demo.order.processing
     * - 动词在前：processing 而非 order.processed
     * - 遵循 Prometheus 命名约定：小写、下划线分隔
     *
     * 【标签设计】
     * 低基数标签（会作为 Prometheus 标签）：
     * - order.type: VIP / STANDARD（只有 2 种值）
     * - order.status: SUCCESS / FAILURE（只有 2 种值）
     *
     * 高基数标签（仅用于追踪，不影响指标）：
     * - order.id: 每个订单唯一（基数 = 订单总数）
     * - order.customer: 客户姓名（基数 = 客户数）
     */
    static class OrderProcessingObservationConvention implements ObservationConvention<Observation.Context> {

        /**
         * 判断是否支持给定的 Context
         * 返回 true 时，此 Convention 会被用于该 Observation
         */
        @Override
        public boolean supportsContext(Observation.Context context) {
            // 通过 context name 来判断
            // 实际项目中可以定义专用的 Context 子类来精确匹配
            return context.getName() != null
                    && context.getName().startsWith("demo.order.");
        }

        /**
         * 指标名称
         * 在 Prometheus 中会变成：
         *   demo_order_processing_duration_seconds (Timer)
         *   demo_order_processing_active (并发数 Gauge)
         */
        @Override
        public String getName() {
            return "demo.order.processing";
        }

        /**
         * 低基数标签 - 用作 Prometheus 标签
         *
         * 注意：这里只能返回有限的标签值集合
         * 如果标签值过多（如 orderId），应该放在 getHighCardinalityKeyValues 中
         */
        @Override
        public KeyValues getLowCardinalityKeyValues(Observation.Context context) {
            // 默认标签
            return KeyValues.of(
                    KeyValue.of("service", "order-service"),
                    KeyValue.of("component", "processing")
            );
        }

        /**
         * 高基数标签 - 仅用于分布式追踪
         * 这些标签不会出现在 Prometheus 指标中
         * 但会出现在 Zipkin/Jaeger 的 Span 中
         */
        @Override
        public KeyValues getHighCardinalityKeyValues(Observation.Context context) {
            // 如果没有具体的订单信息，返回空
            return KeyValues.empty();
        }
    }

    /**
     * =============================================
     * HTTP 请求 ObservationConvention 实现
     * =============================================
     *
     * 演示如何为 HTTP 请求创建自定义 Convention。
     * Spring Boot 已经内置了 ServerHttpObservationConvention，
     * 这里仅作为教学示例。
     */
    static class HttpRequestObservationConvention implements ObservationConvention<Observation.Context> {

        @Override
        public boolean supportsContext(Observation.Context context) {
            return context.getName() != null
                    && context.getName().startsWith("demo.http.");
        }

        @Override
        public String getName() {
            return "demo.http.requests";
        }

        @Override
        public KeyValues getLowCardinalityKeyValues(Observation.Context context) {
            return KeyValues.of(
                    KeyValue.of("service", "demo-service"),
                    KeyValue.of("protocol", "http")
            );
        }
    }
}
