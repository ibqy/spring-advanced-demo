package com.ibqy.spring.advanced.demo08_events.listener;

import com.ibqy.spring.advanced.demo08_events.event.OrderCreatedEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderPaidEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 事件链监听器 —— 追踪订单完整生命周期
 *
 * <p>监听订单的三个阶段事件，形成完整的事件链：
 * <pre>
 *   OrderCreatedEvent ──→ OrderPaidEvent ──→ OrderShippedEvent
 *        │                     │                    │
 *   [阶段1: 创建]         [阶段2: 支付]        [阶段3: 发货]
 * </pre>
 *
 * <h2>设计思想</h2>
 * <p>事件链模式将复杂的业务流程拆解为独立的事件节点。
 * 每个监听器只关心自己负责的阶段，不需要了解全局流程。
 * 这与状态机不同 —— 状态机是集中式的，事件链是分布式的。
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>事件链 vs 状态机？→ 事件链更松耦合，但流程可视化较差</li>
 *     <li>如何追踪事件链的执行状态？→ 用 correlationId（orderId）串联</li>
 *     <li>事件链中某个环节失败怎么办？→ 发布补偿事件（如 OrderPaymentFailedEvent）</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-22
 */
@Component
public class EventChainListener {

    private static final Logger log = LoggerFactory.getLogger(EventChainListener.class);

    @Order(10)
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        System.out.printf("  [事件链-阶段1] 订单 %s 已创建 | 客户: %s | 金额: %.2f%n",
                event.orderId(), event.customer(), event.amount());
        log.info("[事件链] 订单 {} 进入创建阶段", event.orderId());
    }

    @Order(10)
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        System.out.printf("  [事件链-阶段2] 订单 %s 已支付 | 支付单号: %s | 方式: %s%n",
                event.orderId(), event.paymentId(), event.paymentMethod());
        log.info("[事件链] 订单 {} 进入支付阶段", event.orderId());
    }

    @Order(10)
    @EventListener
    public void onOrderShipped(OrderShippedEvent event) {
        System.out.printf("  [事件链-阶段3] 订单 %s 已发货 | 运单号: %s | 承运商: %s%n",
                event.orderId(), event.trackingNo(), event.carrier());
        log.info("[事件链] 订单 {} 进入发货阶段，生命周期完成", event.orderId());
    }
}
