package com.ibqy.spring.advanced.demo02_http_interface;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * ============================================================
 * Demo 02 配置: HTTP Interface Client 配置类
 * ============================================================
 *
 * 【核心原理】
 * HTTP Interface Client 的工作流程：
 *
 *   1. 定义接口（如 GitHubApi）
 *      └── 使用 @HttpExchange / @GetExchange 等注解
 *
 *   2. 创建底层 HTTP 客户端（WebClient / RestClient）
 *      └── WebClient.builder().baseUrl(...).build()
 *
 *   3. 创建适配器（WebClientAdapter / RestClientAdapter）
 *      └── 将 Spring HTTP 客户端适配为 HttpExchangeAdapter 接口
 *
 *   4. 创建代理工厂（HttpServiceProxyFactory）
 *      └── HttpServiceProxyFactory.builderFor(adapter).build()
 *
 *   5. 生成代理对象
 *      └── factory.createClient(GitHubApi.class)
 *      └── 内部使用 JDK 动态代理，将方法调用转换为 HTTP 请求
 *
 * 【Spring Boot 4.1 简化配置】
 * 在 Spring Boot 4.1 中，可以进一步简化：
 *   - 使用 @HttpExchangeClient 注解自动创建代理（7.2 新特性预览）
 *   - 配合 Spring Cloud LoadBalancer 实现客户端负载均衡
 *
 * 【面试考点】
 * 1. HttpServiceProxyFactory 的作用：将接口方法调用转换为 HTTP 请求
 * 2. WebClient vs RestClient 的选择：
 *    - WebClient：响应式（返回 Mono/Flux），适合 WebFlux 项目
 *    - RestClient：同步阻塞，适合传统 Spring MVC 项目（Spring 6.1+）
 * 3. 代理对象的线程安全性：生成的代理对象是线程安全的，可以单例使用
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Configuration
public class HttpInterfaceConfig {

    /**
     * 创建 WebClient Bean
     *
     * WebClient 是 Spring WebFlux 提供的非阻塞 HTTP 客户端，
     * 也是 HTTP Interface Client 的底层实现之一。
     *
     * 【配置说明】
     * - baseUrl：基础 URL，后续请求会拼接在此之后
     * - defaultHeader：默认请求头，所有请求都会携带
     * - codecs：配置 JSON 编解码器的最大缓冲区大小
     *
     * 【面试考点】WebClient vs RestTemplate
     * | 特性         | WebClient          | RestTemplate       |
     * |-------------|--------------------|--------------------|
     * | 阻塞模型     | 非阻塞（Reactor）   | 阻塞               |
     * | 底层实现     | Netty/Reactor Netty | JDK HttpClient     |
     * | 流式支持     | 支持 SSE/Stream    | 不支持              |
     * | 推荐场景     | 高并发、流式场景     | 简单同步调用         |
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                // 基础 URL：GitHub API v3
                .baseUrl("https://api.github.com")
                // 默认请求头
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "Spring-Advanced-Demo/1.0")
                // 如果有 GitHub Token，可以在这里添加认证头
                // .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * 创建 HttpServiceProxyFactory Bean
     *
     * 这是 HTTP Interface Client 的核心工厂类，负责：
     * 1. 解析接口上的 @HttpExchange 等注解
     * 2. 为每个方法生成 HTTP 请求模板
     * 3. 使用 JDK 动态代理生成接口实现
     *
     * 【源码解读】
     * HttpServiceProxyFactory 内部维护了一个 HttpExchangeContract，
     * 它在首次调用时解析接口元数据并缓存，后续调用直接使用缓存。
     *
     * 【Spring Boot 4.1 新特性】
     * 支持 RestClientAdapter（Spring 6.1+），可以在非响应式项目中使用
     * RestClient（同步阻塞）替代 WebClient（响应式非阻塞）作为底层客户端。
     */
    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory(WebClient webClient) {
        // 1. 将 WebClient 包装为适配器
        //    WebClientAdapter 实现了 HttpExchangeAdapter 接口
        //    负责将注解元数据转换为实际的 HTTP 请求
        WebClientAdapter adapter = WebClientAdapter.create(webClient);

        // 2. 构建代理工厂
        return HttpServiceProxyFactory.builderFor(adapter)
                // 可选：配置编解码器
                // .customCodecConfigurer(...)
                .build();
    }

    /**
     * 创建 GitHubApi 代理 Bean
     *
     * 这一步是最终的使用入口：
     * 1. HttpServiceProxyFactory.createClient() 生成 JDK 动态代理
     * 2. 代理对象实现了 GitHubApi 接口
     * 3. 调用接口方法时，代理会：
     *    a. 解析方法参数和注解
     *    b. 构建 HTTP 请求（URL、Header、Body）
     *    c. 通过 WebClient 发送请求
     *    d. 将响应反序列化为返回类型
     *
     * 【面试考点】
     * 为什么不用 OpenFeign？
     * 1. OpenFeign 基于 Netflix，已停止积极维护
     * 2. Spring 官方的 @HttpExchange 更轻量、更集成
     * 3. 支持响应式和同步两种模式
     * 4. 与 Spring Security、CircuitBreaker 等无缝集成
     */
    @Bean
    public GitHubApi gitHubApi(HttpServiceProxyFactory factory) {
        // createClient() 内部使用 JDK 动态代理
        // 等价于：Proxy.newProxyInstance(classLoader, new Class[]{GitHubApi.class}, handler)
        return factory.createClient(GitHubApi.class);
    }
}
