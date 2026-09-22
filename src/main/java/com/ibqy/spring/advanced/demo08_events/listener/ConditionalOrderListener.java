package com.ibqy.spring.advanced.demo08_events.listener;

import com.ibqy.spring.advanced.demo08_events.event.OrderCreatedEvent;
import com.ibqy.spring.advanced.demo08_events.event.OrderPaidEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 条件监听器 —— 基于 SpEL 表达式的事件过滤
 *
 * <h2>@EventListener 的 condition 属性</h2>
 * <p>使用 SpEL（Spring Expression Language）在运行时决定是否执行监听方法。
 * 只有条件为 {@code true} 时，监听器才会被调用。
 *
 * <h2>常用 SpEL 写法</h2>
 * <pre>
 *   #event.amount > 500                    → 按金额过滤
 *   #event.customer == 'VIP'               → 按客户类型过滤
 *   #event.amount > 1000 && #event.customer.startsWith('张')  → 组合条件
 * </pre>
 *
 * <h2>条件监听 vs 方法内 if 判断</h2>
 * <table>
 *     <tr><th>维度</th><th>SpEL condition</th><th>方法内 if</th></tr>
 *     <tr><td>性能</td><td>不满足条件时方法根本不被调用</td><td>方法被调用，只是跳过逻辑</td></tr>
 *     <tr><td>可读性</td><td>注解上直接看到过滤条件</td><td>需要读方法体</td></tr>
 *     <tr><td>复杂度</td><td>适合简单条件</td><td>适合复杂逻辑</td></tr>
 *     <tr><td>可测试性</td><td>条件在注解中，需要集成测试</td><td>可以单元测试 if 分支</td></tr>
 * </table>
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>SpEL 中的 #event 是什么？→ 方法参数的引用，#event 对应参数名 event</li>
 *     <li>条件不满足时，方法会被调用吗？→ 不会，Spring 在代理层就过滤掉了</li>
 *     <li>可以用在 @TransactionalEventListener 上吗？→ 可以</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-22
 */
@Component
public class ConditionalOrderListener {

    private static final Logger log = LoggerFactory.getLogger(ConditionalOrderListener.class);

    /**
     * 大额订单专用监听器
     *
     * <p>只有金额超过 500 的订单才会触发。
     * 适用于需要额外审核或特殊处理的高价值订单。
     */
    @EventListener(condition = "#event.amount() > 500")
    public void onHighValueOrderCreated(OrderCreatedEvent event) {
        System.out.printf("  [条件监听-大额] 订单 %s 金额 %.2f > 500，触发大额订单审核流程%n",
                event.orderId(), event.amount());
        log.warn("[条件监听] 大额订单 {} 需要人工审核，金额: {}", event.orderId(), event.amount());
    }

    /**
     * 小额订单快速通道
     *
     * <p>金额不超过 500 的订单走快速通道。
     */
    @EventListener(condition = "#event.amount() <= 500")
    public void onLowValueOrderCreated(OrderCreatedEvent event) {
        System.out.printf("  [条件监听-小额] 订单 %s 金额 %.2f <= 500，走快速通道%n",
                event.orderId(), event.amount());
    }

    /**
     * 特定支付方式监听
     *
     * <p>只有微信支付才触发风控检查。
     */
    @EventListener(condition = "#event.paymentMethod() == 'WECHAT_PAY'")
    public void onWechatPayment(OrderPaidEvent event) {
        System.out.printf("  [条件监听-微信支付] 订单 %s 使用微信支付，触发风控检查%n",
                event.orderId());
    }

    /**
     * 组合条件示例：大额 + 特定客户
     */
    @EventListener(condition = "#event.amount() > 1000 && #event.customer().startsWith('张')")
    public void onSpecialCase(OrderCreatedEvent event) {
        System.out.printf("  [条件监听-组合] 订单 %s 满足：金额>1000 且客户姓张 → 触发特殊优惠%n",
                event.orderId());
    }
}
