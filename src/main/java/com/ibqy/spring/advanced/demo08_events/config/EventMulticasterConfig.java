package com.ibqy.spring.advanced.demo08_events.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * 自定义事件多播器配置 —— 生产级事件错误隔离
 *
 * <h2>为什么需要自定义 Multicaster？</h2>
 * <p>默认的 {@link SimpleApplicationEventMulticaster} 行为：
 * 如果监听器 A 抛出异常，监听器 B 就不会执行。
 * 这在生产中是不可接受的 —— 一个非关键的审计日志监听器报错，
 * 不应该导致核心的支付通知监听器被跳过。
 *
 * <h2>本配置做了什么？</h2>
 * <ol>
 *     <li><b>异常隔离</b>：设置 ErrorHandler，单个监听器异常不影响其他监听器</li>
 *     <li><b>异步执行器</b>：为 @Async 监听器配置虚拟线程池</li>
 * </ol>
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>ApplicationEventMulticaster 的作用？→ 负责将事件广播给所有匹配的监听器</li>
 *     <li>如何自定义广播策略？→ 实现 ApplicationEventMulticaster 接口或继承 SimpleApplicationEventMulticaster</li>
 *     <li>默认是同步还是异步？→ 同步。设置 TaskExecutor 后变为异步</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-22
 */
@Configuration
public class EventMulticasterConfig {

    private static final Logger log = LoggerFactory.getLogger(EventMulticasterConfig.class);

    /**
     * 自定义事件多播器
     *
     * <p>Bean 名称必须是 {@code applicationEventMulticaster}，Spring 会自动使用它替代默认实现。
     *
     * <p>关键配置：
     * <ul>
     *     <li>{@code setErrorHandler} → 捕获单个监听器的异常，记录日志但不中断广播</li>
     *     <li>{@code setTaskExecutor} → 为所有 @Async 监听器提供虚拟线程执行器</li>
     * </ul>
     */
    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();

        multicaster.setErrorHandler(throwable ->
                log.error("[事件多播器] 监听器执行异常（已隔离，不影响其他监听器）: {}",
                        throwable.getMessage(), throwable)
        );

        multicaster.setTaskExecutor(new VirtualThreadTaskExecutor("event-async-"));

        log.info("[事件多播器] 自定义多播器已初始化：异常隔离 + 虚拟线程池");
        return multicaster;
    }
}
