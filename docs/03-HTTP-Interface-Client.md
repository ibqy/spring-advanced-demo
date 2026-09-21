# 03 · HTTP Interface Client

> 声明式 HTTP 客户端 —— 告别 RestTemplate 的样板代码。

## 概述

Spring Framework 6.0 引入了 **HTTP Interface Client**（声明式 HTTP 客户端），让你像定义 Feign 客户端一样简洁，但更轻量、更灵活。本 Demo 展示如何用 `@HttpExchange` 注解定义一个 HTTP 接口，Spring 自动生成实现类。

## 核心概念

### 与传统方案的对比

| 方案 | 代码量 | 类型安全 | 响应式 | 维护性 |
|------|--------|---------|--------|--------|
| `RestTemplate` | 多 | 弱 | 否 | 差 |
| `WebClient` | 中 | 中 | 是 | 中 |
| `OpenFeign` | 少 | 强 | 否 | 好 |
| **`@HttpExchange`** | **极少** | **强** | **可选** | **极好** |

### 工作原理

```
                     ┌─────────────────────┐
                     │   @HttpExchange     │
                     │   接口定义           │
                     └────────┬────────────┘
                              │
                    Spring AOP 代理
                              │
                     ┌────────▼────────────┐
                     │  HttpExchangeProxy  │
                     │  (运行时生成实现)     │
                     └────────┬────────────┘
                              │
                     ┌────────▼────────────┐
                     │  WebClient /        │
                     │  RestClient         │
                     │  (底层 HTTP 引擎)    │
                     └─────────────────────┘
```

## 代码走读

### 1. 定义 HTTP 接口

```java
// demo02/httpclient/client/UserApiClient.java
@HttpExchange(url = "/api/users")
public interface UserApiClient {

    @GetExchange("")
    List<User> findAll();

    @GetExchange("/{id}")
    User findById(@PathVariable Long id);

    @PostExchange("")
    User create(@RequestBody User user);

    @PutExchange("/{id}")
    User update(@PathVariable Long id, @RequestBody User user);

    @DeleteExchange("/{id}")
    void delete(@PathVariable Long id);

    @GetExchange("/search")
    List<User> search(@RequestParam String name);
}
```

### 2. 配置 WebClient

```java
// demo02/httpclient/config/HttpConfig.java
@Configuration
public class HttpConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse());
    }

    @Bean
    public UserApiClient userApiClient(WebClient.Builder builder) {
        RestClient restClient = builder.build().mutate().build()
                .toRestClient();  // Spring 6.2+ 支持 RestClient

        // 或者使用 WebClient（响应式场景）
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(builder.build()))
                .build();

        return factory.createClient(UserApiClient.class);
    }
}
```

### 3. 在 Controller 中使用

```java
// demo02/httpclient/controller/DemoController.java
@RestController
@RequestMapping("/demo02")
public class DemoController {

    private final UserApiClient userApiClient;

    public DemoController(UserApiClient userApiClient) {
        this.userApiClient = userApiClient;
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userApiClient.findAll();
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userApiClient.findById(id);
    }
}
```

## 注解与 API 参考

| 注解 | 说明 | 等价 HTTP 方法 |
|------|------|---------------|
| `@HttpExchange` | 定义接口级别的 URL 前缀 | — |
| `@GetExchange` | GET 请求 | `GET` |
| `@PostExchange` | POST 请求 | `POST` |
| `@PutExchange` | PUT 请求 | `PUT` |
| `@DeleteExchange` | DELETE 请求 | `DELETE` |
| `@PatchExchange` | PATCH 请求 | `PATCH` |
| `@PathVariable` | 路径变量 | `{id}` |
| `@RequestParam` | 查询参数 | `?key=value` |
| `@RequestBody` | 请求体 | JSON Body |
| `@RequestHeader` | 请求头 | `Header: value` |

## 面试考点

::: warning 高频面试题
1. **HTTP Interface Client 和 OpenFeign 有什么区别？**
   - HTTP Interface 是 Spring 原生方案，Feign 是 Netflix 开源方案
   - HTTP Interface 同时支持 WebClient（响应式）和 RestClient（阻塞式）
   - Feign 功能更丰富（拦截器、契约、编码器/解码器插件体系）
   - 新项目推荐 HTTP Interface，微服务网关场景 Feign 更成熟

2. **底层代理是如何生成的？**
   - 使用 JDK 动态代理（`Proxy.newProxyInstance`）
   - `HttpServiceProxyFactory` 解析接口注解 → 构建 `HttpExchange` 元数据
   - 运行时根据注解信息通过 WebClient/RestClient 发送请求

3. **如何处理错误响应？**
   - 通过 WebClient 的 `onStatus` 钩子处理
   - 或使用 `WebClientResponseException` 全局异常处理
:::

## 常见陷阱

::: danger 陷阱 1：忘记配置 HttpServiceProxyFactory
`@HttpExchange` 接口必须通过 `HttpServiceProxyFactory.createClient()` 创建代理 Bean，不能直接 `@Component` 扫描。
:::

::: danger 陷阱 2：BaseUrl 配置错误
接口上的 `@HttpExchange(url = "/api/users")` 是相对路径，必须配合 `WebClient.Builder.baseUrl()` 使用。如果接口上写绝对路径，BaseUrl 会被覆盖。
:::

::: danger 陷阱 3：序列化问题
确保 WebClient 配置了正确的 `ExchangeStrategies`，对于大响应体需要调整 `maxInMemorySize`：
```java
WebClient.builder()
    .exchangeStrategies(ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs()
            .maxInMemorySize(10 * 1024 * 1024))  // 10MB
        .build())
    .build();
```
:::

## 延伸阅读

- [Spring 官方文档 - HTTP Interfaces](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- [HttpExchange JavaDoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/service/annotation/HttpExchange.html)
- [RestClient vs WebClient 选型指南](https://spring.io/blog/2024/01/16/restclient-rest-template-replacement)
