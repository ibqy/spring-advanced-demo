package com.ibqy.spring.advanced.demo06_mvc_advanced.controller;

import com.ibqy.spring.advanced.demo06_mvc_advanced.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MVC 进阶演示控制器
 *
 * <p>本控制器演示以下 Spring MVC 高级特性：
 * <ul>
 *     <li>SSE (Server-Sent Events) 两种实现方式</li>
 *     <li>异常抛出让全局处理器接管</li>
 *     <li>自定义响应格式</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@RestController
@RequestMapping("/api/demo06")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    /**
     * 定时任务线程池（用于 SSE 推送演示）
     */
    private final ScheduledExecutorService sseScheduler = Executors.newScheduledThreadPool(2);

    // ====================== 基础接口 ======================

    /**
     * 基础接口 —— 测试拦截器是否生效
     */
    @GetMapping("/hello")
    public Map<String, Object> hello(@RequestParam(defaultValue = "World") String name) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "Hello, " + name + "!");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("hint", "请查看响应头 X-Request-Duration，这是 PerformanceInterceptor 添加的");
        return result;
    }

    // ====================== 异常演示 ======================

    /**
     * 抛出业务异常 —— 演示全局异常处理器
     *
     * <p>访问 /api/demo06/error/business 可以看到 GlobalExceptionHandler 的处理结果
     */
    @GetMapping("/error/business")
    public Map<String, Object> triggerBusinessError() {
        // 模拟业务校验失败：抛出带有错误码的业务异常
        throw new BusinessException(1001, "用户不存在，请检查用户ID");
    }

    /**
     * 抛出空指针异常 —— 演示兜底异常处理
     */
    @GetMapping("/error/null")
    public Map<String, Object> triggerNullError() {
        // 模拟意外的系统异常
        String value = null;
        // 这行会抛出 NullPointerException，被 GlobalExceptionHandler 的兜底方法捕获
        return Map.of("length", value.length());
    }

    // ====================== SSE (Server-Sent Events) ======================

    /**
     * SSE 方式一：使用 {@link SseEmitter}（Servlet 栈）
     *
     * <p>SSE 是单向的服务端推送技术，适合：实时通知、股票行情、日志流等场景。
     * <p>相比 WebSocket 更轻量，基于 HTTP 协议，自动重连。
     *
     * <p>客户端测试：
     * <pre>
     *   const eventSource = new EventSource('/api/demo06/sse/subscribe?count=5');
     *   eventSource.onmessage = (event) => console.log(event.data);
     * </pre>
     *
     * @param count 推送消息条数，默认 5
     * @return SseEmitter 对象，Spring 会自动将其注册为 SSE 连接
     */
    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeSse(@RequestParam(defaultValue = "5") int count) {
        // 超时时间设为 60 秒；0 表示不超时
        SseEmitter emitter = new SseEmitter(60_000L);

        log.info("[SSE] 新客户端连接，将推送 {} 条消息", count);

        // 注册回调：连接完成/超时/出错时清理资源
        emitter.onCompletion(() -> log.info("[SSE] 连接已完成"));
        emitter.onTimeout(() -> log.warn("[SSE] 连接超时"));
        emitter.onError(e -> log.error("[SSE] 连接异常", e));

        // 异步推送数据（不阻塞当前请求线程！）
        AtomicInteger counter = new AtomicInteger(0);
        sseScheduler.scheduleAtFixedRate(() -> {
            int current = counter.incrementAndGet();
            try {
                if (current > count) {
                    // 推送完毕，完成连接
                    emitter.complete();
                    return;
                }

                // 构建 SSE 事件数据
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("sequence", current);
                data.put("message", "这是第 " + current + " 条推送消息");
                data.put("serverTime", LocalDateTime.now().toString());

                // 发送事件（id 和 name 是 SSE 协议的标准字段）
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(current))
                        .name("demo-message")
                        .data(data, MediaType.APPLICATION_JSON));

            } catch (IOException | IllegalStateException e) {
                // 客户端断开连接时会抛出异常
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS); // 每秒推送一条

        return emitter;
    }

    /**
     * SSE 方式二：使用 Reactor {@link Flux}（WebFlux 栈）
     *
     * <p>Spring MVC（Servlet 栈）也可以使用 Reactor 的返回类型，
     * Spring 会自动适配为 SSE 流。这种方式代码更简洁。
     *
     * <p>面试考点：Servlet 栈和 WebFlux 栈都能实现 SSE，区别是什么？
     * <ul>
     *     <li>SseEmitter：基于 Servlet 3.1 异步，一个连接占用一个线程</li>
     *     <li>Flux：基于 Reactor 非阻塞，少量线程处理大量连接</li>
     * </ul>
     */
    @GetMapping(value = "/sse/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> subscribeFlux() {
        AtomicInteger counter = new AtomicInteger(0);

        return Flux.interval(Duration.ofSeconds(1))
                .take(10) // 只推送 10 条
                .map(tick -> {
                    int seq = counter.incrementAndGet();
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("sequence", seq);
                    data.put("message", "Flux 推送 #" + seq);
                    data.put("timestamp", LocalDateTime.now().toString());

                    return ServerSentEvent.<Map<String, Object>>builder()
                            .id(String.valueOf(seq))
                            .event("flux-message")
                            .data(data)
                            .build();
                });
    }

    // ====================== 模拟延迟接口（测试拦截器） ======================

    /**
     * 模拟一个慢接口，用于测试 PerformanceInterceptor 的耗时统计
     */
    @GetMapping("/slow/{delayMs}")
    public Map<String, Object> slowEndpoint(@PathVariable int delayMs) throws InterruptedException {
        log.info("[慢接口] 模拟 {}ms 延迟", delayMs);
        Thread.sleep(delayMs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("delay", delayMs + "ms");
        result.put("message", "这是一个慢接口，请查看控制台和响应头中的耗时信息");
        return result;
    }
}
