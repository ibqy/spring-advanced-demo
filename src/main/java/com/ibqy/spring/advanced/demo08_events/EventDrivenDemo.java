package com.ibqy.spring.advanced.demo08_events;

import com.ibqy.spring.advanced.demo08_events.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Demo 08：Spring 事件驱动机制 —— 深度解析
 *
 * <h2>本 Demo 涵盖的核心知识点</h2>
 * <ol>
 *     <li><b>自定义事件</b>：使用 Java record 定义不可变事件对象</li>
 *     <li><b>同步监听</b>：{@code @EventListener} + {@code @Order} 控制顺序</li>
 *     <li><b>异步监听</b>：{@code @Async} + {@code @EventListener}，不阻塞发布者</li>
 *     <li><b>事务感知监听</b>：{@code @TransactionalEventListener} 的四个阶段</li>
 *     <li><b>事件发布</b>：{@code ApplicationEventPublisher} 注入和使用</li>
 * </ol>
 *
 * <h2>事件执行时序图</h2>
 * <pre>
 *   ┌──────────┐                                          ┌──────────┐
 *   │ 发布者    │                                          │ 监听者    │
 *   └────┬─────┘                                          └────┬─────┘
 *        │                                                      │
 *        │  publishEvent(event)                                  │
 *        │─────────────────────────────────────────────────────→ │
 *        │                                                      │ @Order(1) @EventListener
 *        │                                    [同步执行, 阻塞发布者]
 *        │                                                      │ @Order(2) @EventListener
 *        │                                    [同步执行, 阻塞发布者]
 *        │                                                      │ @Async @EventListener
 *        │                                    [提交到线程池, 不阻塞]
 *        │                                                      │
 *        │  [事务进行中]                                          │ @TransactionalEventListener
 *        │                                    BEFORE_COMMIT ←────│ [事务提交前, 阻塞]
 *        │                                                      │
 *        │  [事务提交]                                            │
 *        │                                    AFTER_COMMIT ←─────│ [事务提交后]
 *        │                                                      │
 *        │  [方法返回]                                            │
 *        │←─────────────────────────────────────────────────────  │
 * </pre>
 *
 * <h2>面试高频问题</h2>
 * <ul>
 *     <li>Spring 事件和 Spring Cloud Stream 的区别？→ 前者是进程内通信，后者是跨进程消息传递</li>
 *     <li>事件机制适合什么场景？→ 解耦业务逻辑（下单后发通知、记日志、更新缓存等）</li>
 *     <li>如何保证事件的可靠投递？→ 进程内事件不保证（JVM 重启丢失），需要持久化就用消息队列</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see com.ibqy.spring.advanced.demo08_events.event.OrderCreatedEvent
 * @see com.ibqy.spring.advanced.demo08_events.listener.OrderEventListener
 * @see com.ibqy.spring.advanced.demo08_events.service.OrderService
 */
@SpringBootApplication(scanBasePackages = "com.ibqy.spring.advanced.demo08_events")
public class EventDrivenDemo {

    public static void main(String[] args) {
        SpringApplication.run(EventDrivenDemo.class, args);
    }

    /**
     * 启动后自动执行事件演示
     */
    @Bean
    CommandLineRunner demoRunner(OrderService orderService) {
        return args -> {
            System.out.println("""
                    
                    ╔══════════════════════════════════════════════════════════╗
                    ║     Demo 08: Spring 事件驱动机制                        ║
                    ║     @EventListener · @Async · @TransactionalEvent      ║
                    ╚══════════════════════════════════════════════════════════╝
                    """);

            // ==================== 场景1：成功创建订单（事务内发布事件） ====================
            System.out.println("========== 场景1：成功创建订单（事务内发布事件） ==========");
            System.out.println("  预期行为：");
            System.out.println("    1. @Order(1) 同步监听 → 先执行");
            System.out.println("    2. @Order(2) 同步监听 → 后执行");
            System.out.println("    3. @Async 异步监听 → 线程池异步执行");
            System.out.println("    4. BEFORE_COMMIT → 事务提交前");
            System.out.println("    5. AFTER_COMMIT → 事务提交后");
            System.out.println("    6. AFTER_COMPLETION → 事务完成后");
            System.out.println();

            String orderId = orderService.createOrder("张三", 299.99);
            System.out.printf("%n  [主线程] 订单创建方法已返回，订单号: %s%n", orderId);

            // 等待异步监听器完成
            Thread.sleep(1000);

            // ==================== 场景2：失败创建订单（事务回滚） ====================
            System.out.println("\n========== 场景2：失败创建订单（事务回滚） ==========");
            System.out.println("  预期行为：");
            System.out.println("    1. 同步监听器仍然执行（事件已发布）");
            System.out.println("    2. BEFORE_COMMIT 不执行（事务没有提交）");
            System.out.println("    3. AFTER_ROLLBACK 执行（事务回滚了）");
            System.out.println("    4. AFTER_COMPLETION 执行（无论提交/回滚都执行）");
            System.out.println();

            try {
                orderService.createOrderWithFailure("李四", 599.00);
            } catch (RuntimeException ex) {
                System.out.printf("  [主线程] 捕获到异常: %s%n", ex.getMessage());
            }

            Thread.sleep(1000);

            // ==================== 场景3：非事务方式发布事件 ====================
            System.out.println("\n========== 场景3：非事务方式发布事件 ==========");
            System.out.println("  预期行为：");
            System.out.println("    1. 同步监听器执行");
            System.out.println("    2. 异步监听器执行");
            System.out.println("    3. @TransactionalEventListener 不执行（无事务上下文）");
            System.out.println();

            orderService.publishWithoutTransaction("王五", 99.00);

            Thread.sleep(1000);

            System.out.println("""
                    
                    【面试考点速记】
                    1. 事件机制实现了业务逻辑的解耦 → 下单逻辑不需要知道通知/日志等后续操作
                    2. @TransactionalEventListener 比 @EventListener 更安全 → 保证数据已持久化
                    3. @Async 让监听者不阻塞发布者 → 但要注意线程池配置和异常处理
                    4. 事件顺序用 @Order 控制 → 但异步监听器的顺序不确定
                    5. 事件 vs 消息队列 → 进程内用事件（轻量），跨服务用消息队列（可靠）
                    """);

            // ==================== 场景4：事件链（多阶段生命周期） ====================
            System.out.println("\n========== 场景4：事件链 —— 订单完整生命周期 ==========");
            System.out.println("  预期行为：");
            System.out.println("    1. OrderCreatedEvent → 创建阶段监听器执行");
            System.out.println("    2. OrderPaidEvent → 支付阶段监听器执行");
            System.out.println("    3. OrderShippedEvent → 发货阶段监听器执行");
            System.out.println("    4. 所有阶段通过 orderId 关联（correlationId 模式）");
            System.out.println();

            String chainOrderId = orderService.createOrder("赵六", 1299.00);
            orderService.payOrder(chainOrderId, "ALIPAY", 1299.00);
            orderService.shipOrder(chainOrderId, "顺丰速运");

            Thread.sleep(1000);

            // ==================== 场景5：条件监听（SpEL 过滤） ====================
            System.out.println("\n========== 场景5：条件监听 —— SpEL 表达式过滤 ==========");
            System.out.println("  预期行为：");
            System.out.println("    1. 金额 > 500 → 触发大额订单审核");
            System.out.println("    2. 金额 <= 500 → 走快速通道");
            System.out.println("    3. 条件在 @EventListener(condition=...) 中声明");
            System.out.println();

            System.out.println("  --- 创建大额订单（金额 800）---");
            orderService.createOrder("大额客户A", 800.00);
            Thread.sleep(500);

            System.out.println("  --- 创建小额订单（金额 200）---");
            orderService.createOrder("小额客户B", 200.00);
            Thread.sleep(500);

            // ==================== 场景6：幂等处理（事件去重） ====================
            System.out.println("\n========== 场景6：幂等处理 —— 事件去重 ==========");
            System.out.println("  预期行为：");
            System.out.println("    1. 第一次发布 → 正常处理");
            System.out.println("    2. 重复发布相同 orderId → 跳过处理");
            System.out.println("    3. 生产环境用 Redis 实现分布式去重");
            System.out.println();

            String idempotentId = "ORD-IDEM-001";
            System.out.println("  --- 第一次发布事件 ---");
            orderService.simulateDuplicateEvent(idempotentId, 500.00, "幂等测试客户");
            Thread.sleep(500);

            System.out.println("  --- 第二次发布相同事件（模拟重试）---");
            orderService.simulateDuplicateEvent(idempotentId, 500.00, "幂等测试客户");
            Thread.sleep(500);

            System.out.println("""
                    
                    【进阶模式速记】
                    6. 事件链模式 → 用 orderId 作为 correlationId 串联多阶段事件
                    7. 条件监听 → SpEL 表达式在编译期决定监听器是否执行
                    8. 幂等处理 → ConcurrentHashMap.newKeySet() 去重，生产用 Redis
                    9. 自定义 Multicaster → 异常隔离，一个监听器报错不影响其他
                    10. 生命周期事件 → ApplicationReadyEvent 做启动后初始化
                    """);
        };
    }
}
