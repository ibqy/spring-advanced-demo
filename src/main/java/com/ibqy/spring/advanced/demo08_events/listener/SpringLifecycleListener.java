package com.ibqy.spring.advanced.demo08_events.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring 生命周期事件监听器 —— 监听框架内置事件
 *
 * <h2>Spring Boot 启动时序中的关键事件</h2>
 * <pre>
 *   SpringApplication.run()
 *     │
 *     ├─ ApplicationStartingEvent          ← 应用开始启动
 *     ├─ ApplicationEnvironmentReadyEvent  ← 环境准备完毕
 *     ├─ ApplicationContextInitializedEvent ← 上下文创建
 *     ├─ ContextRefreshedEvent             ← 上下文刷新（Bean 全部加载）
 *     ├─ ApplicationStartedEvent           ← 应用启动完成（CommandLineRunner 之前）
 *     ├─ ApplicationReadyEvent             ← 应用就绪（可以接收请求）
 *     │
 *     └─ ContextClosedEvent                ← 应用关闭
 * </pre>
 *
 * <h2>常见用途</h2>
 * <ul>
 *     <li>{@code ApplicationReadyEvent} → 预热缓存、注册服务发现、发送启动通知</li>
 *     <li>{@code ContextClosedEvent} → 优雅关闭：刷写缓冲区、释放连接、注销注册中心</li>
 *     <li>{@code ContextRefreshedEvent} → 动态刷新配置（注意：可能被多次触发）</li>
 * </ul>
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>ApplicationStartedEvent vs ApplicationReadyEvent？→ Started 在 CommandLineRunner 之前，
 *     Ready 在所有 Runner 执行完毕之后。健康检查应该在 Ready 之后才返回 UP</li>
 *     <li>ContextRefreshedEvent 会触发几次？→ 至少一次。如果有子上下文或 lazy init，可能多次</li>
 *     <li>如何监听两种实现方式？→ 实现 ApplicationListener 接口 或 @EventListener 注解</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-22
 */
@Component
public class SpringLifecycleListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SpringLifecycleListener.class);

    /**
     * 方式一：实现 ApplicationListener 接口（类型安全，只能监听一种事件）
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        System.out.println("  [生命周期] ApplicationReadyEvent → 应用已就绪，可以接收请求");
        log.info("[生命周期] 应用就绪，执行启动后初始化：预热缓存、注册服务发现");
    }

    /**
     * 方式二：@EventListener 注解（灵活，一个类可以监听多种事件）
     */
    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        System.out.println("  [生命周期] ContextRefreshedEvent → 上下文刷新完成，所有 Bean 已加载");
        log.info("[生命周期] 上下文刷新，Bean 数量: {}",
                event.getApplicationContext().getBeanDefinitionCount());
    }

    @EventListener
    public void onApplicationStarted(ApplicationStartedEvent event) {
        System.out.println("  [生命周期] ApplicationStartedEvent → 应用启动完成");
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        System.out.println("  [生命周期] ContextClosedEvent → 应用正在关闭，执行优雅退出");
        log.info("[生命周期] 应用关闭中：刷写缓冲区、释放数据库连接、注销注册中心");
    }
}
