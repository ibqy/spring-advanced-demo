package com.ibqy.spring.advanced.demo03_virtual_threads;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * Demo 03: Virtual Threads (虚拟线程) 并发演示
 * ============================================================
 *
 * 【背景知识】
 * Java 21 (LTS) 正式引入虚拟线程（Virtual Threads, JEP 444）。
 * 虚拟线程是轻量级线程，由 JVM 调度而非操作系统调度。
 *
 * 核心特点：
 * - 创建成本极低：~1KB 内存 vs 平台线程 ~1MB
 * - 可以轻松创建数百万个虚拟线程
 * - I/O 阻塞时不占用载体线程（carrier thread）
 * - 同步代码风格，无需 async/await
 *
 * 【Spring Boot 4.1 集成】
 * 配置 spring.threads.virtual.enabled=true 后：
 * - Tomcat 使用虚拟线程处理请求
 * - Spring MVC 的异步支持自动切换
 * - @Async 可以使用虚拟线程执行器
 * - Spring WebFlux 的 block() 在虚拟线程中更高效
 *
 * 【面试考点】
 * 1. 虚拟线程 vs 协程（Kotlin）：虚拟线程是 JDK 标准，协程是语言特性
 * 2. 虚拟线程 vs 平台线程：调度方式、内存占用、适用场景
 * 3. Pinning 问题：synchronized 和 native 方法会导致虚拟线程钉在载体线程上
 * 4. 适用场景：I/O 密集型（网络请求、文件读写、数据库查询）
 * 5. 不适用场景：CPU 密集型（计算任务用平台线程 + ForkJoinPool 更好）
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
public class VirtualThreadsDemo {

    /**
     * 应用启动后运行演示
     */
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        System.out.println("""
                
                ====================================================
                 Demo 03: Virtual Threads (虚拟线程) 并发演示
                ====================================================
                """);

        // 运行所有演示
        demoThreadCreationComparison();
        demoIoIntensiveSimulation();
        demoVirtualThreadPinning();
        demoSpringBootIntegration();
        demoBestPractices();

        System.out.println("[Demo 03 完成] Virtual Threads 演示结束");
        System.out.println("====================================================");
    }

    /**
     * 演示 1: 平台线程 vs 虚拟线程的创建开销对比
     *
     * 【实验设计】
     * 创建 10000 个任务，对比两种线程的：
     * - 创建时间
     * - 内存占用（估算）
     * - 完成时间
     */
    private void demoThreadCreationComparison() {
        System.out.println("[1] 平台线程 vs 虚拟线程 - 创建开销对比");
        System.out.println("--------------------------------------------");

        int taskCount = 10_000;

        // --- 平台线程测试 ---
        // 注意：创建 10000 个平台线程可能会消耗大量内存（约 10GB）
        // 所以我们使用线程池来限制实际线程数，但比较"提交任务"的时间
        Instant start;
        Instant end;

        // 测试 1a: 使用固定大小的平台线程池
        System.out.println("\n  [平台线程] 使用 FixedThreadPool(200) 提交 " + taskCount + " 个任务...");
        ExecutorService platformExecutor = Executors.newFixedThreadPool(200);
        CountDownLatch platformLatch = new CountDownLatch(taskCount);
        AtomicInteger platformDone = new AtomicInteger(0);

        start = Instant.now();
        for (int i = 0; i < taskCount; i++) {
            platformExecutor.submit(() -> {
                try {
                    // 模拟一个极短的任务
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    platformDone.incrementAndGet();
                    platformLatch.countDown();
                }
            });
        }
        // 等待所有任务提交完成
        Instant platformSubmitEnd = Instant.now();

        try {
            platformLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Instant platformEnd = Instant.now();
        platformExecutor.shutdown();

        long platformSubmitTime = Duration.between(start, platformSubmitEnd).toMillis();
        long platformTotalTime = Duration.between(start, platformEnd).toMillis();
        System.out.println("  提交耗时: " + platformSubmitTime + "ms");
        System.out.println("  总完成耗时: " + platformTotalTime + "ms");
        System.out.println("  线程池大小: 200 (FixedThreadPool)");
        System.out.println("  每个平台线程栈大小: ~1MB (默认)");

        // --- 虚拟线程测试 ---
        System.out.println("\n  [虚拟线程] 使用 VirtualThreadPerTaskExecutor 提交 " + taskCount + " 个任务...");
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch virtualLatch = new CountDownLatch(taskCount);
        AtomicInteger virtualDone = new AtomicInteger(0);

        start = Instant.now();
        for (int i = 0; i < taskCount; i++) {
            virtualExecutor.submit(() -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    virtualDone.incrementAndGet();
                    virtualLatch.countDown();
                }
            });
        }
        Instant virtualSubmitEnd = Instant.now();

        try {
            virtualLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Instant virtualEnd = Instant.now();
        virtualExecutor.shutdown();

        long virtualSubmitTime = Duration.between(start, virtualSubmitEnd).toMillis();
        long virtualTotalTime = Duration.between(start, virtualEnd).toMillis();
        System.out.println("  提交耗时: " + virtualSubmitTime + "ms");
        System.out.println("  总完成耗时: " + virtualTotalTime + "ms");
        System.out.println("  每个虚拟线程栈大小: ~几KB (按需增长)");

        // --- 对比总结 ---
        System.out.println("""
                
                  [对比总结]
                  +------------------+------------------+------------------+
                  | 指标             | 平台线程          | 虚拟线程          |
                  +------------------+------------------+------------------+
                  | 提交 """ + taskCount + """
                 个任务  | """ + String.format("%-16s", platformSubmitTime + "ms") + """
                  | """ + String.format("%-16s", virtualSubmitTime + "ms") + """
                  |
                  | 全部完成          | """ + String.format("%-16s", platformTotalTime + "ms") + """
                  | """ + String.format("%-16s", virtualTotalTime + "ms") + """
                  |
                  | 内存/线程         | ~1MB             | ~几KB             |
                  | 调度方式          | OS 内核调度       | JVM 用户态调度     |
                  | 适用上限          | 数千个            | 数百万个           |
                  +------------------+------------------+------------------+
                """);
    }

    /**
     * 演示 2: 模拟 I/O 密集型场景
     *
     * 【实验设计】
     * 模拟 1000 个并发 HTTP 请求（每个请求 sleep 50ms 模拟 I/O 等待）
     * 对比两种线程模型下的吞吐量
     */
    private void demoIoIntensiveSimulation() {
        System.out.println("""
                [2] I/O 密集型场景模拟 - 1000 个并发请求
                --------------------------------------------""");

        int requestCount = 1000;
        int ioWaitMs = 50; // 模拟每次 I/O 等待 50ms

        // --- 平台线程（线程池 200）---
        ExecutorService platformExecutor = Executors.newFixedThreadPool(200);
        CountDownLatch latch1 = new CountDownLatch(requestCount);

        Instant start = Instant.now();
        for (int i = 0; i < requestCount; i++) {
            platformExecutor.submit(() -> {
                try {
                    // 模拟 I/O 操作（数据库查询、HTTP 调用等）
                    Thread.sleep(ioWaitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch1.countDown();
                }
            });
        }
        try {
            latch1.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long platformTime = Duration.between(start, Instant.now()).toMillis();
        platformExecutor.shutdown();

        // --- 虚拟线程 ---
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch2 = new CountDownLatch(requestCount);

        start = Instant.now();
        for (int i = 0; i < requestCount; i++) {
            virtualExecutor.submit(() -> {
                try {
                    Thread.sleep(ioWaitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch2.countDown();
                }
            });
        }
        try {
            latch2.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long virtualTime = Duration.between(start, Instant.now()).toMillis();
        virtualExecutor.shutdown();

        System.out.println("  平台线程(200): " + platformTime + "ms (受限于线程池大小)");
        System.out.println("  虚拟线程:      " + virtualTime + "ms (全部并发执行)");
        System.out.println("""
                
                  [分析]
                  - 平台线程池 200 个线程，每个 I/O 50ms
                  - 理论最短时间: 1000/200 * 50 = 250ms
                  - 虚拟线程 1000 个全部并发
                  - 理论最短时间: ~50ms（全部同时执行）
                  - 结论: I/O 密集型场景，虚拟线程吞吐量显著更高
                """);
    }

    /**
     * 演示 3: 虚拟线程 Pinning（钉住）问题
     *
     * 【重要概念】
     * 当虚拟线程执行 synchronized 块或 native 方法时，
     * 会被"钉住"(pinned)在载体线程上，无法 unmount。
     * 这会导致性能下降，因为载体线程被阻塞了。
     *
     * 【解决方案】
     * 1. 使用 ReentrantLock 替代 synchronized
     * 2. 避免在虚拟线程中调用 native 方法
     * 3. 使用 jdk.virtualThreadScheduler 的 diagnostics 监控
     */
    private void demoVirtualThreadPinning() {
        System.out.println("""
                [3] 虚拟线程 Pinning (钉住) 问题演示
                --------------------------------------------
                
                【什么是 Pinning？】
                虚拟线程在以下情况会被"钉住"在载体线程上：
                1. 执行 synchronized 代码块
                2. 调用 native 方法（JNI）
                
                【为什么会发生？】
                虚拟线程的调度原理：
                - 虚拟线程运行在载体线程（carrier thread）上
                - 遇到 I/O 阻塞时，虚拟线程会 unmount（卸载）
                - 载体线程可以去执行其他虚拟线程
                - 但 synchronized 内部使用 OS 的 monitor，无法 unmount
                
                【影响】
                - 少量 pinning 通常不影响性能
                - 大量 pinning 会导致载体线程耗尽，性能退化
                
                【解决方案】
                \s""" + """
                // 不推荐 (会导致 pinning):
                synchronized (lock) {
                    httpClient.send(request, bodyHandler); // I/O 操作
                }
                
                // 推荐 (不会 pinning):
                ReentrantLock lock = new ReentrantLock();
                lock.lock();
                try {
                    httpClient.send(request, bodyHandler);
                } finally {
                    lock.unlock();
                }
                """);

        // 实际演示 pinning 检测
        System.out.println("  演示 synchronized 导致的 Pinning:");
        Object lock = new Object();

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch pinLatch = new CountDownLatch(100);

        Instant start = Instant.now();
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                synchronized (lock) {
                    try {
                        // 在 synchronized 块中做 I/O 模拟
                        // 这会导致虚拟线程被 pin 在载体线程上
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                pinLatch.countDown();
            });
        }
        try {
            pinLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long pinTime = Duration.between(start, Instant.now()).toMillis();
        executor.shutdown();

        // 对比：使用 ReentrantLock
        java.util.concurrent.locks.ReentrantLock reentrantLock = new java.util.concurrent.locks.ReentrantLock();
        ExecutorService executor2 = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch noPinLatch = new CountDownLatch(100);

        start = Instant.now();
        for (int i = 0; i < 100; i++) {
            executor2.submit(() -> {
                reentrantLock.lock();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    reentrantLock.unlock();
                }
                noPinLatch.countDown();
            });
        }
        try {
            noPinLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long noPinTime = Duration.between(start, Instant.now()).toMillis();
        executor2.shutdown();

        System.out.println("  synchronized 耗时: " + pinTime + "ms (虚拟线程被 pin 住)");
        System.out.println("  ReentrantLock 耗时: " + noPinTime + "ms (虚拟线程可以自由调度)");
        System.out.println("""
                
                  [注意] 两者在这个 Demo 中差异可能不大，
                  但在高并发 I/O 场景下（如 10000+ 虚拟线程），
                  synchronized 的 pinning 效应会显著影响性能。
                """);
    }

    /**
     * 演示 4: Spring Boot 4.1 集成虚拟线程
     *
     * 【配置方式】
     * application.yml:
     *   spring.threads.virtual.enabled: true
     *
     * 【影响范围】
     * - Tomcat 请求处理线程 -> 虚拟线程
     * - Spring MVC 异步处理 -> 虚拟线程
     * - @Async -> 可配置为虚拟线程执行器
     * - @Scheduled -> Spring 6.2+ 支持虚拟线程
     */
    private void demoSpringBootIntegration() {
        System.out.println("""
                [4] Spring Boot 4.1 集成虚拟线程
                --------------------------------------------
                
                === 配置方式 ===
                
                # application.yml
                spring:
                  threads:
                    virtual:
                      enabled: true  # 一行配置启用虚拟线程
                
                === 启用后的效果 ===
                
                1. Tomcat 请求处理
                   - 每个 HTTP 请求由虚拟线程处理（而非平台线程池）
                   - 不再受限于 server.tomcat.threads.max (默认 200)
                   - 可以同时处理数万个并发请求
                
                2. @Async 异步方法
                   - 需要配置虚拟线程执行器：
                   \s""" + """
                     @Bean
                     public Executor virtualThreadExecutor() {
                         return Executors.newVirtualThreadPerTaskExecutor();
                     }
                   \s""" + """
                   - 或者使用 Spring Boot 4.1 的自动配置
                
                3. @Scheduled 定时任务
                   - Spring Boot 4.1 自动使用虚拟线程
                   - 无需额外配置
                
                4. 数据访问层
                   - JDBC 连接池需要配合虚拟线程使用
                   - HikariCP 4.0+ 支持虚拟线程
                
                === 代码示例：@Async + 虚拟线程 ===
                \s""" + """
                // 配置虚拟线程执行器
                @Configuration
                @EnableAsync
                public class AsyncConfig {
                    @Bean
                    public Executor virtualThreadTaskExecutor() {
                        return Executors.newVirtualThreadPerTaskExecutor();
                    }
                }
                
                // 使用 @Async
                @Service
                public class MyService {
                    @Async("virtualThreadTaskExecutor")
                    public CompletableFuture<String> asyncMethod() {
                        // 这个方法会在虚拟线程中执行
                        return CompletableFuture.completedFuture("done");
                    }
                }
                """);
    }

    /**
     * 演示 5: 虚拟线程最佳实践
     */
    private void demoBestPractices() {
        System.out.println("""
                [5] 虚拟线程最佳实践 & 面试考点总结
                --------------------------------------------
                
                === 适用场景（推荐使用虚拟线程） ===
                1. 大量并发 I/O 操作（HTTP 请求、数据库查询、文件读写）
                2. 需要简单同步风格的并发代码（不需要 async/await）
                3. 微服务网关/代理（大量转发请求）
                4. 批量数据处理（每个任务等待外部资源）
                
                === 不适用场景（不要使用虚拟线程） ===
                1. CPU 密集型计算（虚拟线程没有性能优势）
                2. 需要精确控制线程数量的场景
                3. 使用了大量 native 方法（JNI）
                4. ThreadLocal 使用过多的代码（虚拟线程数量巨大）
                
                === 面试高频问题 ===
                
                Q: 虚拟线程是协程吗？
                A: 从概念上类似，但 Java 虚拟线程是 JDK 标准 API。
                   Kotlin 协程是编译器特性，需要 suspend 关键字。
                   虚拟线程对代码完全透明，不需要改编程模型。
                
                Q: 虚拟线程和 CompletableFuture 怎么选？
                A: - 新代码推荐虚拟线程（代码更简洁）
                   - CompletableFuture 适合已有响应式基础设施的项目
                   - 两者可以混合使用
                
                Q: 虚拟线程的调度原理是什么？
                A: - JVM 维护一个 ForkJoinPool 作为载体线程池
                   - 每个虚拟线程任务分配给一个载体线程执行
                   - I/O 阻塞时，虚拟线程 unmount，载体线程可执行其他任务
                   - I/O 完成后，虚拟线程重新 mount 到载体线程
                
                Q: 如何监控虚拟线程？
                A: - jcmd Thread.print 可以查看所有虚拟线程
                   - JFR (Java Flight Recorder) 有虚拟线程事件
                   - Spring Boot Actuator 的 /actuator/threaddump
                
                Q: spring.threads.virtual.enabled=true 的底层实现？
                A: Tomcat 10.1+ 原生支持虚拟线程。
                   Spring Boot 通过设置 Tomcat 的 threadFactory 为虚拟线程工厂。
                   每个 HTTP 连接由一个虚拟线程处理，而非平台线程。
                """);
    }
}
