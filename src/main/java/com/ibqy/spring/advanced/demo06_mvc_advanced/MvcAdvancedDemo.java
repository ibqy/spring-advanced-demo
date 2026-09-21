package com.ibqy.spring.advanced.demo06_mvc_advanced;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Demo 06：Spring MVC 进阶 —— 高级特性总览
 *
 * <h2>本 Demo 涵盖的核心知识点</h2>
 * <ol>
 *     <li><b>HandlerInterceptor</b>：请求拦截器，用于日志、鉴权、耗时统计</li>
 *     <li><b>@RestControllerAdvice</b>：全局异常处理，统一错误响应格式</li>
 *     <li><b>WebMvcConfigurer</b>：CORS 跨域、日期格式化、拦截器注册</li>
 *     <li><b>SSE (Server-Sent Events)</b>：服务端推送，SseEmitter + Flux 两种方式</li>
 *     <li><b>HttpMessageConverter</b>：自定义消息转换器（见 {@link com.ibqy.spring.advanced.demo06_mvc_advanced.config.WebMvcConfig}）</li>
 * </ol>
 *
 * <h2>请求处理全流程</h2>
 * <pre>
 *   HTTP 请求
 *      │
 *      ▼
 *   Filter（Servlet 过滤器链）
 *      │
 *      ▼
 *   DispatcherServlet
 *      │
 *      ▼
 *   HandlerMapping → 找到对应的 Controller 方法
 *      │
 *      ▼
 *   HandlerInterceptor#preHandle ──[返回 false]──→ afterCompletion（结束）
 *      │                                          ↑
 *      ▼                                          │
 *   HttpMessageConverter → 参数反序列化              │
 *      │                                          │
 *      ▼                                          │
 *   Controller 方法执行                            │
 *      │                                          │
 *      ├──[正常]──→ postHandle → 视图渲染 → afterCompletion
 *      │                                          │
 *      └──[异常]──→ HandlerExceptionResolver ──────┘
 *                       │
 *                       ▼
 *                   @RestControllerAdvice 中的 @ExceptionHandler
 *                       │
 *                       ▼
 *                   HttpMessageConverter → 响应序列化 → HTTP 响应
 * </pre>
 *
 * <h2>面试高频问题</h2>
 * <ul>
 *     <li>Spring MVC 的核心组件有哪些？→ DispatcherServlet、HandlerMapping、HandlerAdapter、ViewResolver</li>
 *     <li>@RestController 和 @Controller 的区别？→ 前者自带 @ResponseBody，方法返回值直接写入响应体</li>
 *     <li>Spring MVC 的线程模型？→ 每个请求一个线程（Servlet 线程池），配合虚拟线程可提升并发能力</li>
 * </ul>
 *
 * <h2>测试接口列表</h2>
 * <ul>
 *     <li>GET /api/demo06/hello?name=Spring → 基础接口（查看响应头）</li>
 *     <li>GET /api/demo06/error/business → 触发业务异常</li>
 *     <li>GET /api/demo06/error/null → 触发系统异常</li>
 *     <li>GET /api/demo06/sse/subscribe?count=5 → SSE 订阅（SseEmitter）</li>
 *     <li>GET /api/demo06/sse/flux → SSE 订阅（Reactor Flux）</li>
 *     <li>GET /api/demo06/slow/500 → 慢接口（测试耗时统计）</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see com.ibqy.spring.advanced.demo06_mvc_advanced.interceptor.PerformanceInterceptor
 * @see com.ibqy.spring.advanced.demo06_mvc_advanced.exception.GlobalExceptionHandler
 * @see com.ibqy.spring.advanced.demo06_mvc_advanced.config.WebMvcConfig
 * @see com.ibqy.spring.advanced.demo06_mvc_advanced.controller.DemoController
 */
@SpringBootApplication(scanBasePackages = "com.ibqy.spring.advanced.demo06_mvc_advanced")
public class MvcAdvancedDemo implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(MvcAdvancedDemo.class, args);
    }

    private final ConfigurableApplicationContext context;

    public MvcAdvancedDemo(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @PostConstruct
    void printBanner() {
        System.out.println("""
                
                ╔══════════════════════════════════════════════════════════╗
                ║     Demo 06: Spring MVC 进阶                           ║
                ║     HandlerInterceptor · 全局异常 · CORS · SSE         ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }

    @Override
    public void run(String... args) {
        System.out.println("""
                
                ┌──────────────────────────────────────────────────────────┐
                │  Demo 06 已启动！可用的测试接口：                         │
                ├──────────────────────────────────────────────────────────┤
                │  GET /api/demo06/hello?name=Spring   基础接口            │
                │  GET /api/demo06/error/business      业务异常演示        │
                │  GET /api/demo06/error/null          系统异常演示        │
                │  GET /api/demo06/sse/subscribe       SSE (SseEmitter)   │
                │  GET /api/demo06/sse/flux            SSE (Reactor Flux) │
                │  GET /api/demo06/slow/500            慢接口（耗时统计）   │
                ├──────────────────────────────────────────────────────────┤
                │  已注册的 MVC 组件：                                      │
                """);

        // 列出所有 WebMvcConfigurer Bean
        String[] configurerNames = context.getBeanNamesForType(WebMvcConfigurer.class);
        System.out.printf("  │    WebMvcConfigurer: %d 个%n", configurerNames.length);
        for (String name : configurerNames) {
            System.out.printf("  │      - %s%n", name);
        }

        System.out.println("""
                └──────────────────────────────────────────────────────────┘
                
                【面试考点速记】
                1. Interceptor vs Filter → Interceptor 在 DispatcherServlet 内工作，可获取 Handler 信息
                2. @RestControllerAdvice → 全局异常处理的推荐方式，比 Filter 更灵活
                3. CORS 配置 → 生产环境务必限制 allowedOrigins，不要用 "*"
                4. SSE vs WebSocket → SSE 单向推送更轻量，WebSocket 双向通信更灵活
                """);
    }
}
