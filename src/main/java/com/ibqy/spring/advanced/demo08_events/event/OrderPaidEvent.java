package com.ibqy.spring.advanced.demo08_events.event;

import java.time.LocalDateTime;

/**
 * 订单已支付事件 —— 事件链的第二阶段
 *
 * <p>订单生命周期：{@code OrderCreatedEvent → OrderPaidEvent → OrderShippedEvent}
 *
 * <p>面试考点：为什么把支付和创建分成两个事件而不是一个？
 * → 单一职责原则。创建事件关注"订单生成了"，支付事件关注"钱到账了"。
 * 监听者可以只关心其中一个阶段，比如物流系统只需要监听支付事件。
 *
 * @author ibqy
 * @since 2026-09-22
 */
public record OrderPaidEvent(
        String orderId,
        String paymentId,
        double amount,
        String paymentMethod,
        LocalDateTime timestamp
) {

    public OrderPaidEvent(String orderId, String paymentId, double amount, String paymentMethod) {
        this(orderId, paymentId, amount, paymentMethod, LocalDateTime.now());
    }
}
