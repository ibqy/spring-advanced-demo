package com.ibqy.spring.advanced.demo09_actuator;

import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Collection;
import java.util.List;

/**
 * Demo 09：Spring Boot Actuator 自定义扩展
 *
 * <h2>本 Demo 涵盖的核心知识点</h2>
 * <ol>
 *     <li><b>自定义端点</b>：{@code @Endpoint} + {@code @ReadOperation} / {@code @WriteOperation} / {@code @DeleteOperation}</li>
 *     <li><b>自定义 HealthIndicator</b>：检查外部依赖健康状态，集成到 /actuator/health</li>
 *     <li><b>自定义 InfoContributor</b>：向 /actuator/info 添加自定义信息</li>
 *     <li><b>EndpointWebExtension</b>：为已有端点添加 Web 特有功能</li>
 * </ol>
 *
 * <h2>Actuator 端点一览</h2>
 * <pre>
 *   端点 ID          HTTP 方法     描述
 *   ──────────────────────────────────────────────
 *   health           GET          应用健康状态
 *   info             GET          应用信息
 *   metrics          GET          应用指标（Micrometer）
 *   env              GET          环境变量
 *   beans            GET          所有 Spring Bean
 *   appInfo          GET/POST/DEL ← 我们自定义的！
 * </pre>
 *
 * <h2>测试方式</h2>
 * <pre>
 *   # 查看自定义端点
 *   curl http://localhost:8080/actuator/appInfo
 *
 *   # 查看健康状态（含自定义 HealthIndicator）
 *   curl http://localhost:8080/actuator/health
 *
 *   # 更新配置
 *   curl -X POST "http://localhost:8080/actuator/appInfo?key=maintenance.mode&value=true"
 *
 *   # 删除配置
 *   curl -X DELETE http://localhost:8080/actuator/appInfo/maintenance.mode
 * </pre>
 *
 * <h2>面试高频问题</h2>
 * <ul>
 *     <li>Actuator 的安全性？→ 生产环境必须限制访问！通常配合 Spring Security 做鉴权，
 *     或者将管理端点放在不同的端口（management.server.port=8081）</li>
 *     <li>如何自定义端点路径？→ management.endpoints.web.base-path=/manage</li>
 *     <li>InfoContributor 和自定义端点有什么区别？→ InfoContributor 是往 /actuator/info 添加信息，
 *     自定义端点是完全独立的 URL</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see com.ibqy.spring.advanced.demo09_actuator.endpoint.AppInfoEndpoint
 * @see com.ibqy.spring.advanced.demo09_actuator.endpoint.AppInfoEndpointWebExtension
 * @see com.ibqy.spring.advanced.demo09_actuator.health.ExternalApiHealthIndicator
 */
@SpringBootApplication(scanBasePackages = "com.ibqy.spring.advanced.demo09_actuator")
public class CustomActuatorDemo {

    public static void main(String[] args) {
        SpringApplication.run(CustomActuatorDemo.class, args);
    }

    /**
     * 启动后打印自定义端点信息
     */
    @Bean
    CommandLineRunner demoRunner(EndpointDiscoverer<?, ?> endpointDiscoverer) {
        return args -> {
            System.out.println("""
                    
                    ╔══════════════════════════════════════════════════════════╗
                    ║     Demo 09: 自定义 Actuator 端点                       ║
                    ║     @Endpoint · HealthIndicator · WebExtension          ║
                    ╚══════════════════════════════════════════════════════════╝
                    """);

            // 列出所有注册的端点
            Collection<? extends ExposableEndpoint<?>> endpoints = endpointDiscoverer.getEndpoints();
            System.out.println("========== 已注册的 Actuator 端点 ==========");
            List<String> endpointNames = endpoints.stream()
                    .map(e -> e.getEndpointId().toString())
                    .sorted()
                    .toList();
            for (String name : endpointNames) {
                String marker = name.equals("appInfo") ? " ★ 自定义" : "";
                System.out.printf("  - /actuator/%s%s%n", name, marker);
            }

            System.out.println("""
                    
                    ========== 自定义端点使用指南 ==========
                    
                    1. 查看应用信息:
                       GET http://localhost:8080/actuator/appInfo
                    
                    2. 查询特定配置:
                       GET http://localhost:8080/actuator/appInfo/feature.darkMode
                    
                    3. 更新配置:
                       POST http://localhost:8080/actuator/appInfo
                       Body: key=maintenance.mode&value=true
                    
                    4. 删除配置:
                       DELETE http://localhost:8080/actuator/appInfo/maintenance.mode
                    
                    5. 查看健康状态（含自定义 HealthIndicator）:
                       GET http://localhost:8080/actuator/health
                    
                    【面试考点速记】
                    1. @Endpoint 是协议无关的 → 同时支持 HTTP 和 JMX
                    2. HealthIndicator 聚合到 /actuator/health → K8s 的 liveness/readiness probe 使用它
                    3. 生产环境必须保护 Actuator 端点 → 用 Spring Security 或 management.server.port 隔离
                    4. @EndpointWebExtension → 为端点添加 HTTP 特有功能（状态码、响应头等）
                    """);
        };
    }
}
