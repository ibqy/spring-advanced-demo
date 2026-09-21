package com.ibqy.spring.advanced.demo06_mvc_advanced.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 性能监控拦截器 —— 统计每个请求的处理耗时
 *
 * <p>{@link HandlerInterceptor} 是 Spring MVC 提供的拦截机制，工作在 DispatcherServlet 层面，
 * 比 Filter 更细粒度（可以拿到 Handler 信息），比 AOP 更轻量（不需要代理）。
 *
 * <h3>执行流程</h3>
 * <pre>
 *   请求 → preHandle() → Controller → postHandle() → 视图渲染 → afterCompletion()
 *        ↑                                                                     ↓
 *        └──────────────────── 完整请求生命周期 ──────────────────────────────────┘
 * </pre>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>Interceptor vs Filter？→ Filter 是 Servlet 规范，Interceptor 是 Spring MVC 概念，可获取 Handler 信息</li>
 *     <li>preHandle 返回 false 会怎样？→ 请求终止，不会执行 Controller，直接调用 afterCompletion</li>
 *     <li>多个 Interceptor 的执行顺序？→ preHandle 按注册顺序，postHandle/afterCompletion 按逆序</li>
 *     <li>为什么用 ThreadLocal 而不是实例变量？→ 多线程环境下实例变量会被共享，ThreadLocal 保证线程隔离</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PerformanceInterceptor.class);

    /**
     * 用 ThreadLocal 存储每个请求的开始时间
     * <p>为什么用 ThreadLocal？因为 Servlet 容器使用线程池处理请求，
     * 同一个 Interceptor 实例会被多个线程并发使用，必须保证数据隔离。
     */
    private static final ThreadLocal<Long> START_TIME_HOLDER = new ThreadLocal<>();

    /**
     * 请求头：记录请求开始时间（供前端查看）
     */
    private static final String HEADER_REQUEST_DURATION = "X-Request-Duration";

    /**
     * 请求开始前的回调
     *
     * @return true 表示放行继续执行；false 表示拦截请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录开始时间（纳秒精度，避免 System.currentTimeMillis() 受系统时钟调整影响）
        START_TIME_HOLDER.set(System.nanoTime());

        // 在响应头中标记请求已开始（方便前端调试）
        response.setHeader("X-Request-Start", String.valueOf(System.currentTimeMillis()));

        log.debug("[性能监控] 请求开始: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    /**
     * Controller 执行完毕、视图渲染之前的回调
     * <p>注意：只在 preHandle 返回 true 时才会执行
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        // 这里可以做一些日志记录，比如记录返回的视图名称
        if (modelAndView != null) {
            log.debug("[性能监控] 返回视图: {}", modelAndView.getViewName());
        }
    }

    /**
     * 整个请求完成后的回调（无论是否发生异常）
     * <p>这是记录耗时的最佳位置，因为此时请求已经彻底完成。
     * <p><b>重要：</b>必须在 finally 块中调用 remove()，否则会导致内存泄漏！
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        try {
            Long startTime = START_TIME_HOLDER.get();
            if (startTime != null) {
                // 计算耗时（毫秒）
                long durationNanos = System.nanoTime() - startTime;
                double durationMs = durationNanos / 1_000_000.0;

                // 设置响应头，前端可以通过它看到请求耗时
                response.setHeader(HEADER_REQUEST_DURATION, String.format("%.2fms", durationMs));

                // 根据耗时分级记录日志
                if (durationMs > 1000) {
                    log.warn("[性能监控] 慢请求! {} {} 耗时: {}ms", request.getMethod(),
                            request.getRequestURI(), String.format("%.2f", durationMs));
                } else {
                    log.debug("[性能监控] {} {} 耗时: {}ms", request.getMethod(),
                            request.getRequestURI(), String.format("%.2f", durationMs));
                }

                System.out.printf("[性能监控] %s %s → 耗时: %.2fms%n",
                        request.getMethod(), request.getRequestURI(), durationMs);
            }
        } finally {
            // 必须清理！否则线程池复用线程时会导致数据污染和内存泄漏
            START_TIME_HOLDER.remove();
        }
    }
}
