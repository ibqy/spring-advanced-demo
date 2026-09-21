package com.ibqy.spring.advanced.demo06_mvc_advanced.config;

import com.ibqy.spring.advanced.demo06_mvc_advanced.interceptor.PerformanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;

/**
 * Spring MVC 全局配置
 *
 * <p>实现 {@link WebMvcConfigurer} 接口来定制 Spring MVC 的各种行为。
 * 在 Spring Boot 中，只要声明一个实现此接口的 @Configuration 就会自动生效，
 * 不需要额外的 @EnableWebMvc（加了反而会禁用自动配置！）。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>@EnableWebMvc 和 @EnableWebMvc 的区别？→ Spring Boot 中千万别加 @EnableWebMvc，
 *     它会禁用 Spring Boot 的自动配置（如消息转换器的默认配置）</li>
 *     <li>WebMvcConfigurer vs WebMvcConfigurationSupport？→ 前者是扩展点（叠加式），
 *     后者会覆盖所有默认配置。Spring Boot 推荐使用 WebMvcConfigurer</li>
 *     <li>多个 WebMvcConfigurer 如何共存？→ Spring 会按 @Order 排序后依次调用</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;

    /**
     * 通过构造器注入拦截器（Spring 推荐的依赖注入方式）
     */
    public WebMvcConfig(PerformanceInterceptor performanceInterceptor) {
        this.performanceInterceptor = performanceInterceptor;
    }

    /**
     * 注册拦截器
     *
     * <p>拦截器的执行顺序：
     * <pre>
     * addPathPatterns 匹配 → preHandle → Controller → postHandle → afterCompletion
     * </pre>
     *
     * <p>面试考点：excludePathPatterns 常用来排除静态资源路径，
     * 避免拦截器对 /css/**, /js/** 等路径生效。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/api/**")    // 只拦截 /api/ 下的请求
                .excludePathPatterns("/api/demo06/sse/**"); // SSE 长连接不走性能统计
    }

    /**
     * 配置 CORS（跨域资源共享）
     *
     * <p>浏览器的同源策略会阻止前端 JS 跨域请求。CORS 是 W3C 标准的解决方案。
     * <p>Spring MVC 的 CORS 配置会自动处理 OPTIONS 预检请求。
     *
     * <p>面试考点：
     * <ul>
     *     <li>简单请求 vs 预检请求？→ 非 GET/POST/HEAD 或带自定义头的请求会先发 OPTIONS</li>
     *     <li>allowCredentials(true) 时 allowedOrigins 不能用 "*"！→ 必须指定具体域名</li>
     *     <li>CORS 是浏览器端安全机制 → 服务端配置只是告诉浏览器"我允许"，不阻止非浏览器的跨域请求</li>
     * </ul>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173") // 前端开发服务器
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Duration", "X-Request-Start") // 暴露自定义响应头给前端
                .allowCredentials(true)
                .maxAge(3600); // 预检结果缓存 1 小时，减少 OPTIONS 请求
    }

    /**
     * 配置日期时间格式化
     *
     * <p>当 Controller 方法参数是 LocalDate/LocalDateTime 类型时，
     * Spring 需要知道如何把字符串 "2026-09-21 10:00:00" 转换成 LocalDateTime 对象。
     *
     * <p>面试考点：除了这里的全局配置，还可以用 {@code @DateTimeFormat} 注解在参数级别指定格式。
     * 局部注解优先级高于全局配置。
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HH:mm:ss"));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        registrar.registerFormatters(registry);
    }
}
