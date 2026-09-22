package com.ibqy.spring.advanced.demo08_events.event;

import java.time.LocalDateTime;

/**
 * 订单已发货事件 —— 事件链的第三阶段
 *
 * <p>完整的订单事件链：
 * <pre>
 *   OrderCreatedEvent ──→ OrderPaidEvent ──→ OrderShippedEvent
 *        │                     │                    │
 *   [记录审计日志]         [发送支付确认]        [发送物流通知]
 *   [风控检查]            [更新库存]           [触发签收超时计时]
 * </pre>
 *
 * @author ibqy
 * @since 2026-09-22
 */
public record OrderShippedEvent(
        String orderId,
        String trackingNo,
        String carrier,
        LocalDateTime timestamp
) {

    public OrderShippedEvent(String orderId, String trackingNo, String carrier) {
        this(orderId, trackingNo, carrier, LocalDateTime.now());
    }
}
