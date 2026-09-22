package com.ibqy.spring.advanced.demo08_events.service;

import com.ibqy.spring.advanced.demo08_events.event.OrderCreatedEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderPaidEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 订单服务 —— 演示事件发布与事务的结合
 *
 * <h3>核心概念</h3>
 * <p>{@link ApplicationEventPublisher} 是 Spring 的事件发布接口。
 * 所有 ApplicationContext 都实现了这个接口，所以可以直接注入。
 *
 * <h3>事件发布流程</h3>
 * <pre>
 *   1. 调用 publisher.publishEvent(event)
 *   2. Spring 找到所有匹配的 @EventListener 方法
 *   3. 按 @Order 顺序执行同步监听者（在发布者线程中）
 *   4. 异步监听者被提交到线程池
 *   5. @TransactionalEventListener 等待事务阶段触发
 * </pre>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>ApplicationEventPublisher vs ApplicationEventMulticaster？→ Publisher 是面向开发者的接口，
 *     Multicaster 是底层实现（负责广播事件到多个监听者）</li>
 *     <li>发布事件需要事务吗？→ 不需要。但如果监听者用了 @TransactionalEventListener，
 *     则事件必须在事务中发布才会触发</li>
 *     <li>事件的传播范围？→ 默认在同一 ApplicationContext 内传播，不会跨应用</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    /**
     * Spring 的事件发布器
     * <p>面试考点：为什么注入 ApplicationEventPublisher 而不是 ApplicationContext？
     * → 因为 ApplicationEventPublisher 是更精确的接口，遵循接口隔离原则。
     * ApplicationContext 虽然也能发布事件，但它包含了太多不相关的方法。
     */
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 创建订单（成功场景）
     *
     * <p>在事务中发布事件，所有 @TransactionalEventListener 都会被触发。
     *
     * @param customer 客户名称
     * @param amount   订单金额
     * @return 订单 ID
     */
    @Transactional
    public String createOrder(String customer, double amount) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[订单服务] 创建订单: {}, 客户: {}, 金额: {}", orderId, customer, amount);

        // 模拟业务处理...
        System.out.printf("  [OrderService] 订单 %s 创建成功，准备发布事件...%n", orderId);

        // 发布事件
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, amount, customer);
        eventPublisher.publishEvent(event);

        System.out.printf("  [OrderService] 事件已发布，等待事务提交...%n");

        // 注意：此时事务还未提交，@TransactionalEventListener(AFTER_COMMIT) 尚未执行
        // 当这个方法返回后，Spring 会提交事务，然后触发 AFTER_COMMIT 监听器

        return orderId;
    }

    /**
     * 创建订单（失败场景 —— 模拟异常导致事务回滚）
     *
     * <p>方法执行中抛出异常，事务会回滚。
     * <p>此时 @TransactionalEventListener 的 AFTER_COMMIT 不会触发，
     * 而是触发 AFTER_ROLLBACK。
     *
     * @param customer 客户名称
     * @param amount   订单金额
     * @return 不会返回（总是抛出异常）
     */
    @Transactional
    public String createOrderWithFailure(String customer, double amount) {
        String orderId = "ORD-FAIL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[订单服务] 创建订单（将失败）: {}, 客户: {}", orderId, customer);
        System.out.printf("  [OrderService] 订单 %s 创建中...%n", orderId);

        // 先发布事件（此时事务还未完成）
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, amount, customer);
        eventPublisher.publishEvent(event);

        // 模拟业务失败
        System.out.printf("  [OrderService] 模拟业务异常！事务将回滚...%n");
        throw new RuntimeException("模拟异常：支付网关不可用");
    }

    /**
     * 在非事务上下文中发布事件
     *
     * <p>注意：@TransactionalEventListener 默认不会触发（因为不在事务中）。
     * 除非设置了 fallbackExecution = true。
     *
     * <p>面试考点：什么场景需要非事务发布事件？
     * → 启动时的事件、定时任务触发的事件、从消息队列消费的事件等。
     */
    public void publishWithoutTransaction(String customer, double amount) {
        String orderId = "ORD-NT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[订单服务] 非事务方式发布事件: {}", orderId);
        System.out.printf("  [OrderService] 非事务方式发布事件: %s%n", orderId);

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, amount, customer);
        eventPublisher.publishEvent(event);

        System.out.println("  [OrderService] 注意：@TransactionalEventListener 默认不会触发！");
        System.out.println("  [OrderService] 只有 @EventListener 和 @Async 监听器会收到事件");
    }

    /**
     * 支付订单（事件链第二阶段）
     *
     * <p>发布 {@link OrderPaidEvent}，触发支付确认、库存更新等下游操作。
     *
     * @param orderId       订单 ID（与创建事件的 orderId 一致，用于事件关联）
     * @param paymentMethod 支付方式（ALIPAY / WECHAT_PAY / CREDIT_CARD）
     * @param amount        支付金额
     */
    @Transactional
    public void payOrder(String orderId, String paymentMethod, double amount) {
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[订单服务] 订单 {} 支付: 方式={}, 金额={}", orderId, paymentMethod, amount);
        System.out.printf("  [OrderService] 订单 %s 支付成功，支付单号: %s%n", orderId, paymentId);

        OrderPaidEvent event = new OrderPaidEvent(orderId, paymentId, amount, paymentMethod);
        eventPublisher.publishEvent(event);
    }

    /**
     * 发货（事件链第三阶段）
     *
     * <p>发布 {@link OrderShippedEvent}，触发物流通知、签收超时计时等操作。
     *
     * @param orderId   订单 ID
     * @param carrier   承运商
     * @return 运单号
     */
    @Transactional
    public String shipOrder(String orderId, String carrier) {
        String trackingNo = "SF" + UUID.randomUUID().toString().substring(0, 10).toUpperCase().replace("-", "");

        log.info("[订单服务] 订单 {} 发货: 承运商={}, 运单号={}", orderId, carrier, trackingNo);
        System.out.printf("  [OrderService] 订单 %s 已发货，运单号: %s%n", orderId, trackingNo);

        OrderShippedEvent event = new OrderShippedEvent(orderId, trackingNo, carrier);
        eventPublisher.publishEvent(event);

        return trackingNo;
    }

    /**
     * 模拟重复发布同一事件（演示幂等处理）
     *
     * <p>实际场景中，重复事件通常来自消息队列重试或网络重发。
     */
    public void simulateDuplicateEvent(String orderId, double amount, String customer) {
        log.info("[订单服务] 模拟重复事件: {}", orderId);
        System.out.printf("  [OrderService] 模拟重复发布订单 %s 的创建事件...%n", orderId);

        OrderCreatedEvent event = new OrderCreatedEvent(orderId, amount, customer);
        eventPublisher.publishEvent(event);
    }
}
