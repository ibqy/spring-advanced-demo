package com.ibqy.spring.advanced.demo08_events.listener;

import com.ibqy.spring.advanced.demo08_events.event.OrderCreatedEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderPaidEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等事件监听器 —— 防止重复处理
 *
 * <h2>为什么需要幂等？</h2>
 * <p>在以下场景中，同一个事件可能被监听器收到多次：
 * <ul>
 *     <li>消息队列重试（网络超时后重发）</li>
 *     <li>Spring 事件被多个 Multicaster 广播（配置错误）</li>
 *     <li>应用重启后重放事件</li>
 * </ul>
 *
 * <h2>幂等策略</h2>
 * <p>使用 {@link Set} 记录已处理的事件 ID，重复事件直接跳过。
 * 生产环境应使用 Redis 实现分布式去重。
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>进程内去重 vs 分布式去重？→ 进程内用 ConcurrentHashMap，分布式用 Redis SETNX</li>
 *     <li>去重记录的过期策略？→ 设置 TTL（如 24 小时），避免内存无限增长</li>
 *     <li>幂等和重试的关系？→ 幂等是重试的前提，没有幂等就不敢重试</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-22
 */
@Component
public class IdempotentOrderListener {

    private static final Logger log = LoggerFactory.getLogger(IdempotentOrderListener.class);

    private final Set<String> processedOrderIds = ConcurrentHashMap.newKeySet();

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        String orderId = event.orderId();

        if (!processedOrderIds.add(orderId)) {
            System.out.printf("  [幂等监听] 订单 %s 已处理过，跳过重复事件%n", orderId);
            log.warn("[幂等] 重复的订单创建事件: {}", orderId);
            return;
        }

        System.out.printf("  [幂等监听] 首次处理订单 %s，执行库存扣减%n", orderId);
        log.info("[幂等] 处理订单创建，执行库存扣减: {}", orderId);
    }

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        String key = event.orderId() + ":PAID";

        if (!processedOrderIds.add(key)) {
            System.out.printf("  [幂等监听] 订单 %s 支付事件已处理，跳过%n", event.orderId());
            return;
        }

        System.out.printf("  [幂等监听] 首次处理订单 %s 支付，更新支付状态%n", event.orderId());
    }

    @EventListener
    public void onOrderShipped(OrderShippedEvent event) {
        String key = event.orderId() + ":SHIPPED";

        if (!processedOrderIds.add(key)) {
            System.out.printf("  [幂等监听] 订单 %s 发货事件已处理，跳过%n", event.orderId());
            return;
        }

        System.out.printf("  [幂等监听] 首次处理订单 %s 发货，发送物流通知%n", event.orderId());
    }

    /**
     * 查询已处理的事件数量（用于演示和监控）
     */
    public int getProcessedCount() {
        return processedOrderIds.size();
    }
}
