package com.ibqy.spring.advanced.demo08_events.listener;

import com.ibqy.spring.advanced.demo08_events.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单事件监听器 —— 演示 Spring 事件监听的多种方式
 *
 * <h2>四种监听方式对比</h2>
 * <table>
 *     <tr><th>注解</th><th>执行时机</th><th>线程</th><th>事务感知</th></tr>
 *     <tr><td>@EventListener</td><td>事件发布后立即执行</td><td>发布者线程</td><td>否</td></tr>
 *     <tr><td>@Async + @EventListener</td><td>事件发布后异步执行</td><td>线程池新线程</td><td>否</td></tr>
 *     <tr><td>@TransactionalEventListener(AFTER_COMMIT)</td><td>事务提交后执行</td><td>发布者线程</td><td>是</td></tr>
 *     <tr><td>@TransactionalEventListener(BEFORE_COMMIT)</td><td>事务提交前执行</td><td>发布者线程</td><td>是</td></tr>
 * </table>
 *
 * <h2>@Order 顺序控制</h2>
 * <p>同一个事件可以有多个监听者，通过 {@code @Order} 控制执行顺序。
 * <p>数值越小优先级越高：{@code @Order(1)} 先于 {@code @Order(2)} 执行。
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>@EventListener vs @TransactionalEventListener？→ 前者不关心事务，后者感知事务阶段</li>
 *     <li>@TransactionalEventListener 在事务外发布事件会怎样？→ 默认不执行（fallbackExecution=true 时才执行）</li>
 *     <li>异步监听者的异常处理？→ 异步方法的异常不会传播到发布者，需要自己处理</li>
 *     <li>事件监听者可以发布新事件吗？→ 可以，但要注意避免无限循环</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see OrderCreatedEvent
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    // ====================== 1. 同步监听（默认） ======================

    /**
     * 同步监听器 #1（最先执行）
     *
     * <p>{@code @Order(1)} 确保这个监听器在其它同类型监听器之前执行。
     * <p>执行在发布者的线程中，会阻塞发布者直到执行完毕。
     */
    @Order(1)
    @EventListener
    public void onOrderCreatedFirst(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [同步监听-Order1] 线程: %s | 收到订单事件: %s, 金额: %.2f%n",
                threadName, event.orderId(), event.amount());
        log.debug("[同步监听-Order1] 处理订单创建，准备记录审计日志");
        // 模拟审计日志记录
    }

    /**
     * 同步监听器 #2（后执行）
     */
    @Order(2)
    @EventListener
    public void onOrderCreatedSecond(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [同步监听-Order2] 线程: %s | 收到订单事件: %s, 客户: %s%n",
                threadName, event.orderId(), event.customer());
        log.debug("[同步监听-Order2] 处理订单创建，准备发送通知");
    }

    // ====================== 2. 异步监听 ======================

    /**
     * 异步监听器
     *
     * <p>{@code @Async} 让监听器在线程池中异步执行，不阻塞发布者。
     * <p>需要主类或配置类上有 {@code @EnableAsync}（本项目已在主类启用）。
     *
     * <p>面试考点：
     * <ul>
     *     <li>异步监听用的哪个线程池？→ 默认的 SimpleAsyncTaskExecutor（每次创建新线程），
     *     生产环境应该自定义 ThreadPoolTaskExecutor</li>
     *     <li>@Async 和 @Order 一起用，@Order 还有效吗？→ 无效！异步方法的执行顺序不确定</li>
     *     <li>Spring Boot 虚拟线程模式下，@Async 还有必要吗？→ 虚拟线程本身就是轻量级的，
     *     @Async 仍然有用（可以将监听逻辑与请求线程解耦）</li>
     * </ul>
     */
    @Async
    @EventListener
    public void onOrderCreatedAsync(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [异步监听] 线程: %s | 收到订单事件: %s (异步处理中...)%n",
                threadName, event.orderId());
        try {
            // 模拟耗时操作（如调用外部 API 发送通知）
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("  [异步监听] 线程: %s | 订单 %s 异步处理完成%n",
                threadName, event.orderId());
    }

    // ====================== 3. 事务感知监听 ======================

    /**
     * 事务提交前监听器
     *
     * <p>{@code BEFORE_COMMIT}：在事务提交之前执行，仍在事务内。
     * <p>如果这个监听器抛出异常，会导致事务回滚！
     *
     * <p>适用场景：在事务提交前做最后的校验或数据补充。
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderBeforeCommit(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [事务监听-BEFORE_COMMIT] 线程: %s | 订单 %s 即将提交%n",
                threadName, event.orderId());
        log.debug("[事务监听] 在事务提交前执行，可以影响事务结果");
    }

    /**
     * 事务提交后监听器
     *
     * <p>{@code AFTER_COMMIT}：在事务成功提交后执行。
     * <p>此时事务已经提交，这个监听器的异常不会影响事务结果。
     *
     * <p>适用场景：发送通知邮件、更新缓存等"事务完成后"的操作。
     * <p>面试考点：这是最常用的事务监听阶段，因为它保证数据已经持久化。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderAfterCommit(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [事务监听-AFTER_COMMIT] 线程: %s | 订单 %s 已提交，发送确认通知%n",
                threadName, event.orderId());
        log.debug("[事务监听] 事务已提交，数据安全持久化，可以安全地执行后续操作");
    }

    /**
     * 事务回滚后监听器
     *
     * <p>{@code AFTER_ROLLBACK}：在事务回滚后执行。
     * <p>适用场景：记录失败日志、发送告警。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onOrderAfterRollback(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [事务监听-AFTER_ROLLBACK] 线程: %s | 订单 %s 事务已回滚！%n",
                threadName, event.orderId());
        log.warn("[事务监听] 订单创建失败，事务已回滚");
    }

    /**
     * 事务完成后监听器（无论提交还是回滚）
     *
     * <p>{@code AFTER_COMPLETION}：事务完成后的回调，无论是提交还是回滚都会执行。
     * <p>适用场景：清理资源、释放锁。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void onOrderAfterCompletion(OrderCreatedEvent event) {
        String threadName = Thread.currentThread().getName();
        System.out.printf("  [事务监听-AFTER_COMPLETION] 线程: %s | 订单 %s 事务已完成%n",
                threadName, event.orderId());
        log.debug("[事务监听] 事务完成（提交或回滚），执行清理操作");
    }
}
