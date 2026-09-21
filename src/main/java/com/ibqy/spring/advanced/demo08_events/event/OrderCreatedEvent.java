package com.ibqy.spring.advanced.demo08_events.event;

import java.time.LocalDateTime;

/**
 * 订单创建事件 —— 自定义 Spring 应用事件
 *
 * <h3>Spring 事件机制概述</h3>
 * <pre>
 *   发布者 (Publisher)               监听者 (Listener)
 *   ┌─────────────────┐             ┌─────────────────────┐
 *   │ ApplicationEvent │ ──发布──→   │ @EventListener      │
 *   │ Publisher        │             │ @Async (异步)       │
 *   └─────────────────┘             │ @TransactionalEvent  │
 *                                    │ @Order (顺序)       │
 *                                    └─────────────────────┘
 * </pre>
 *
 * <h3>事件设计原则</h3>
 * <ul>
 *     <li>事件应该是不可变的（immutable）→ 发布后不应被修改</li>
 *     <li>事件应该包含足够的上下文 → 监听者需要的所有信息</li>
 *     <li>事件名应该有明确的语义 → OrderCreatedEvent 比 OrderEvent 更好</li>
 * </ul>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>为什么用 record？→ 不可变、自动生成 equals/hashCode/toString、代码简洁</li>
 *     <li>Spring 事件是同步还是异步？→ 默认同步（发布者在同一线程等待所有监听者执行完毕），
 *     加 @Async 可变为异步</li>
 *     <li>事件监听者抛异常会影响发布者吗？→ 同步模式下会！异步模式下不会</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
public record OrderCreatedEvent(

        /**
         * 订单 ID
         */
        String orderId,

        /**
         * 订单金额
         */
        double amount,

        /**
         * 客户名称
         */
        String customer,

        /**
         * 事件发生时间
         */
        LocalDateTime timestamp
) {

    /**
     * 便捷构造方法（自动设置时间戳）
     */
    public OrderCreatedEvent(String orderId, double amount, String customer) {
        this(orderId, amount, customer, LocalDateTime.now());
    }
}
