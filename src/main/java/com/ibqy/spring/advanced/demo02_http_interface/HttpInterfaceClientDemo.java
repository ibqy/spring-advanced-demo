package com.ibqy.spring.advanced.demo02_http_interface;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * Demo 02: HTTP Interface Client 演示
 * ============================================================
 *
 * 【教学目标】
 * 演示 Spring Framework 6.0+ 的声明式 HTTP 客户端用法。
 * 对比传统 WebClient 手动调用与声明式接口调用的区别。
 *
 * 【运行前提】
 * 需要网络连接可以访问 https://api.github.com
 * 公开 API 不需要认证（有速率限制 60 次/小时）
 *
 * 【核心代码结构】
 *   GitHubApi.java          -> 声明式接口（@HttpExchange）
 *   HttpInterfaceConfig.java -> 配置类（WebClient + ProxyFactory）
 *   HttpInterfaceClientDemo.java -> 本文件，调用演示
 *
 * 【面试考点】
 * 1. 声明式 HTTP 客户端的核心价值：接口即文档，编译期类型安全
 * 2. 底层代理机制：JDK 动态代理 vs CGLIB
 * 3. 与 OpenFeign 的差异（见 HttpInterfaceConfig 注释）
 * 4. 错误处理策略：全局 ErrorHandler vs 局部 try-catch
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
public class HttpInterfaceClientDemo {

    /**
     * 注入声明式 HTTP 客户端代理
     * Spring 容器中只有一个 GitHubApi 的 Bean，由 HttpInterfaceConfig 创建
     */
    private final GitHubApi gitHubApi;

    /**
     * 显式构造函数注入（教学规范：不使用 Lombok）
     *
     * 【面试考点】构造器注入 vs 字段注入
     * - 构造器注入：不可变（final）、可测试、明确依赖关系
     * - 字段注入（@Autowired）：可变、难测试、隐藏依赖
     * - Spring 官方推荐构造器注入
     */
    public HttpInterfaceClientDemo(GitHubApi gitHubApi) {
        this.gitHubApi = gitHubApi;
    }

    /**
     * 应用启动完成后自动运行演示
     *
     * 【@EventListener vs @PostConstruct】
     * - @PostConstruct：Bean 初始化完成后执行，此时 ApplicationContext 可能还没完全就绪
     * - @EventListener(ApplicationReadyEvent.class)：整个应用完全启动后才执行，更安全
     * - 对于需要调用外部服务的 Demo，用 ApplicationReadyEvent 更合适
     */
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        System.out.println("""
                
                ====================================================
                 Demo 02: HTTP Interface Client 演示
                ====================================================
                """);

        // =============================================
        // 演示 1: 获取 GitHub 用户信息
        // =============================================
        System.out.println("[1] 声明式 GET 请求 - 获取 GitHub 用户信息");
        System.out.println("--------------------------------------------");

        try {
            // 声明式调用：一行代码完成 HTTP GET + JSON 反序列化
            // 对比传统方式：
            //   WebClient.get()
            //     .uri("/users/{name}", "ibqy")
            //     .retrieve()
            //     .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            //     .block();
            Map<String, Object> user = gitHubApi.getUser("ibqy");

            System.out.println("用户名: " + user.get("login"));
            System.out.println("昵称: " + user.get("name"));
            System.out.println("仓库数: " + user.get("public_repos"));
            System.out.println("简介: " + user.get("bio"));
            System.out.println("头像: " + user.get("avatar_url"));
        } catch (Exception e) {
            System.out.println("请求失败（可能是网络问题或速率限制）: " + e.getMessage());
        }

        // =============================================
        // 演示 2: 获取用户的仓库列表（带查询参数）
        // =============================================
        System.out.println("""
                
                [2] 声明式 GET 请求 - 获取用户仓库列表（带查询参数）
                --------------------------------------------""");

        try {
            // 带查询参数的声明式调用
            // 接口方法定义了 @RequestParam，框架自动拼接 URL
            // 最终请求：GET /users/ibqy/repos?sort=updated&per_page=5
            List<Map<String, Object>> repos = gitHubApi.getUserRepos("ibqy", "updated", 5);

            System.out.println("最近的 " + repos.size() + " 个仓库:");
            for (int i = 0; i < repos.size(); i++) {
                Map<String, Object> repo = repos.get(i);
                System.out.printf("  %d. %s ( Stars: %s, Language: %s )%n",
                        i + 1,
                        repo.getOrDefault("name", "N/A"),
                        repo.getOrDefault("stargazers_count", "0"),
                        repo.getOrDefault("language", "N/A"));
            }
        } catch (Exception e) {
            System.out.println("请求失败: " + e.getMessage());
        }

        // =============================================
        // 演示 3: 搜索仓库
        // =============================================
        System.out.println("""
                
                [3] 声明式 GET 请求 - 搜索仓库
                --------------------------------------------""");

        try {
            // 搜索 GitHub 仓库
            // 最终请求：GET /search/repositories?q=spring+boot&page=1
            Map<String, Object> searchResult = gitHubApi.searchRepos("spring boot", 1);

            System.out.println("搜索 'spring boot' 结果:");
            System.out.println("  总数: " + searchResult.get("total_count"));
            System.out.println("  不完整结果: " + searchResult.get("incomplete_results"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) searchResult.get("items");
            if (items != null) {
                System.out.println("  前 3 个结果:");
                int limit = Math.min(3, items.size());
                for (int i = 0; i < limit; i++) {
                    Map<String, Object> item = items.get(i);
                    System.out.printf("    %d. %s ( Stars: %s )%n",
                            i + 1,
                            item.getOrDefault("full_name", "N/A"),
                            item.getOrDefault("stargazers_count", "0"));
                }
            }
        } catch (Exception e) {
            System.out.println("请求失败: " + e.getMessage());
        }

        // =============================================
        // 对比演示: 传统方式 vs 声明式
        // =============================================
        System.out.println("""
                
                [4] 传统 WebClient vs 声明式 HTTP Client 对比
                --------------------------------------------
                
                === 传统 WebClient 方式（命令式） ===
                Mono<Map<String, Object>> result = webClient.get()
                    .uri("/users/{username}", "ibqy")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
                Map<String, Object> user = result.block(); // 阻塞等待
                
                // 问题：
                // 1. URL 硬编码，难以维护
                // 2. 返回类型需要 ParameterizedTypeReference 处理泛型
                // 3. 每个调用点都要写完整的请求构建代码
                // 4. 没有编译期检查，URL 拼错只有运行时才发现
                
                === 声明式 HTTP Client 方式（接口驱动） ===
                Map<String, Object> user = gitHubApi.getUser("ibqy");
                
                // 优势：
                // 1. 接口即文档，一目了然
                // 2. 编译期类型安全，IDE 自动补全
                // 3. URL 和参数集中管理，易于维护
                // 4. 方便 Mock 测试（直接 Mock 接口即可）
                // 5. 支持 Spring 的 AOP、拦截器等机制
                """);

        // =============================================
        // 面试总结
        // =============================================
        System.out.println("""
                [5] 面试考点总结
                --------------------------------------------
                Q: @HttpExchange 和 @RequestMapping 有什么区别？
                A: - @RequestMapping：标注在 Controller 上，定义服务端如何处理请求
                   - @HttpExchange：标注在接口上，定义客户端如何发送请求
                   - 两者互补：一个面向服务端（Spring MVC），一个面向客户端
                   - 注解名称刻意对应：@GetExchange <-> @GetMapping
                
                Q: HttpServiceProxyFactory 的代理机制是什么？
                A: JDK 动态代理。要求被代理的类型必须是接口。
                   底层流程：方法调用 -> 解析注解 -> 构建请求 -> 发送 -> 反序列化
                
                Q: 如何做错误处理？
                A: 1. 全局：配置 WebClient 的 onStatus() 处理器
                   2. 接口级：实现 ErrorHandler 接口并注册到 ProxyFactory
                   3. 调用级：try-catch 捕获 WebClientResponseException
                
                Q: 如何做认证？
                A: 1. 全局：WebClient.builder().defaultHeader("Authorization", ...)
                   2. 请求级：接口方法添加 @RequestHeader 参数
                   3. 拦截器：实现 ClientHttpRequestInterceptor（同步模式）
                """);

        System.out.println("[Demo 02 完成] HTTP Interface Client 演示结束");
        System.out.println("====================================================");
    }
}
