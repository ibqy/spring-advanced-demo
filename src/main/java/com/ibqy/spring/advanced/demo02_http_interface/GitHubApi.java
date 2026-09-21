package com.ibqy.spring.advanced.demo02_http_interface;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * Demo 02: 声明式 HTTP 客户端接口定义
 * ============================================================
 *
 * 【核心概念】
 * Spring Framework 6.0 引入了 @HttpExchange 注解族，实现了声明式 HTTP 客户端。
 * 这是 Spring 官方的"轻量级 Feign"，优势：
 *   1. 不依赖第三方库（OpenFeign 已停止维护）
 *   2. 与 Spring 生态无缝集成（WebClient / RestClient / RestTemplate）
 *   3. 支持响应式（WebClient）和同步（RestClient）两种模式
 *   4. 编译期类型安全，IDE 自动补全
 *
 * 【注解体系】
 * - @HttpExchange：标注在接口上，定义基础 URL 和公共 Headers
 * - @GetExchange：GET 请求
 * - @PostExchange：POST 请求
 * - @PutExchange：PUT 请求
 * - @DeleteExchange：DELETE 请求
 * - @PatchExchange：PATCH 请求
 *
 * 【与 OpenFeign 的对比】
 * | 特性           | @HttpExchange         | OpenFeign            |
 * |---------------|-----------------------|----------------------|
 * | 维护方         | Spring 官方            | Netflix (已停止维护)   |
 * | 底层客户端     | WebClient/RestClient  | 自研 HTTP 客户端       |
 * | 响应式支持     | 支持（WebClient）       | 不支持                |
 * | 负载均衡       | 需配合 LoadBalancer   | 内置 Ribbon/SLB       |
 * | 学习成本       | 低（Spring 注解风格）   | 中                    |
 *
 * 【面试考点】
 * 1. @HttpExchange 的工作原理：JDK 动态代理 -> HttpServiceProxyFactory
 * 2. 与 @RequestMapping 的区别：一个面向客户端，一个面向服务端
 * 3. 参数绑定：@PathVariable / @RequestParam / @RequestBody / @RequestHeader
 * 4. 错误处理：默认抛出 WebClientResponseException，可自定义 ErrorHandler
 *
 * @author ibqy
 * @since 2026-09-21
 */
@HttpExchange(url = "https://api.github.com", contentType = "application/json")
public interface GitHubApi {

    /**
     * GET 请求：获取用户信息
     *
     * 等价于：
     * WebClient.get()
     *   .uri("https://api.github.com/users/{username}", username)
     *   .retrieve()
     *   .bodyToMono(Map.class)
     *
     * @param username GitHub 用户名
     * @return 用户信息的 Map 表示
     */
    @GetExchange("/users/{username}")
    Map<String, Object> getUser(@org.springframework.web.bind.annotation.PathVariable String username);

    /**
     * GET 请求：获取用户的仓库列表
     *
     * 等价于：
     * GET https://api.github.com/users/{username}/repos?sort=updated&per_page=5
     *
     * @param username 用户名
     * @param sort     排序字段
     * @param perPage  每页数量
     * @return 仓库列表
     */
    @GetExchange("/users/{username}/repos")
    List<Map<String, Object>> getUserRepos(
            @org.springframework.web.bind.annotation.PathVariable String username,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "updated") String sort,
            @org.springframework.web.bind.annotation.RequestParam(name = "per_page", defaultValue = "5") int perPage
    );

    /**
     * POST 请求：创建 Gist（需要认证）
     * 演示 @PostExchange 的用法
     *
     * @param gist 请求体（JSON 格式）
     * @return 创建结果
     */
    @PostExchange("/gists")
    Map<String, Object> createGist(@RequestBody Map<String, Object> gist);

    /**
     * GET 请求：搜索仓库
     * 演示复杂查询参数的处理
     *
     * @param query 搜索关键词
     * @param page  页码
     * @return 搜索结果
     */
    @GetExchange("/search/repositories")
    Map<String, Object> searchRepos(
            @org.springframework.web.bind.annotation.RequestParam("q") String query,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page
    );

    /**
     * DELETE 请求：删除 Gist（需要认证）
     * 演示 @DeleteExchange 的用法
     *
     * @param gistId Gist ID
     */
    @DeleteExchange("/gists/{gist_id}")
    void deleteGist(@org.springframework.web.bind.annotation.PathVariable("gist_id") String gistId);

    /**
     * PUT 请求：标星一个仓库（需要认证）
     * 演示 @PutExchange 的用法
     *
     * @param owner 仓库所有者
     * @param repo  仓库名
     */
    @PutExchange("/user/starred/{owner}/{repo}")
    void starRepo(
            @org.springframework.web.bind.annotation.PathVariable String owner,
            @org.springframework.web.bind.annotation.PathVariable String repo
    );
}
