# 01 · 整体架构

> 理解项目全局结构，是深入学习每个 Demo 的前提。

## 项目定位

Spring Advanced Demo 是一个 **教学优先** 的 Spring Boot 4.1.1 项目，覆盖 10 个高级特性领域。每个 Demo 相互独立但共享同一套基础设施（数据库、安全配置、监控端点），可以单独运行或整体启动。

## 架构图

```
                          ┌─────────────────────────────────────────┐
                          │          Spring Boot 4.1.1              │
                          │         (Auto-Configuration)            │
                          └─────────────────┬───────────────────────┘
                                            │
            ┌───────────────────────────────┼───────────────────────────────┐
            │                               │                               │
    ┌───────▼───────┐              ┌────────▼────────┐             ┌────────▼────────┐
    │   通信层       │              │   Web & 数据层   │             │   框架机制层     │
    │               │              │                 │             │                 │
    │ Demo01: gRPC  │              │ Demo05: 可观测性 │             │ Demo08: 事件驱动 │
    │ Demo02: HTTP  │              │ Demo06: MVC进阶  │             │ Demo09: Actuator │
    │   Interface   │              │ Demo07: JPA进阶  │             │                 │
    └───────────────┘              └─────────────────┘             └─────────────────┘
                                                                    │
    ┌───────────────┐              ┌─────────────────┐             ┌─▼───────────────┐
    │   运行时优化   │              │   安全层         │             │   基础设施       │
    │               │              │                 │             │                 │
    │ Demo03: 虚拟  │              │ Demo10:         │             │ Java 21         │
    │   线程        │              │   Security 6    │             │ H2 Database     │
    │ Demo04: AOT & │              │   进阶          │             │ Micrometer      │
    │   Native      │              │                 │             │ Maven           │
    └───────────────┘              └─────────────────┘             └─────────────────┘
```

## 技术栈详情

| 技术 | 版本 | 在本项目中的角色 |
|------|------|-----------------|
| **Java 21** | LTS | 虚拟线程、Records、模式匹配、文本块 |
| **Spring Boot** | 4.1.1 | 自动配置、Starter、Actuator |
| **Spring Framework** | 7.0.8 | IoC 容器、AOP、MVC、Security Core |
| **Spring Data JPA** | 4.1.x | 数据访问、Specification、Auditing |
| **Spring Security** | 6.4.x | Lambda DSL、JWT、方法级安全 |
| **gRPC** | 1.68.1 | 高性能 RPC 通信 |
| **Protobuf** | 3.25.5 | 序列化协议 |
| **Micrometer** | — | 指标采集门面 |
| **Prometheus** | — | 指标存储与查询 |
| **H2** | — | 内存数据库（教学演示） |

## 项目结构

```
spring-advanced-demo/
├── src/main/java/com/ibqy/spring/advanced/
│   ├── SpringAdvancedDemoApplication.java      # 启动类
│   ├── demo01_grpc/                            # gRPC 服务端（文档 02）
│   │   ├── GrpcServerConfig.java               # 自动配置扩展点
│   │   └── GrpcServerDemo.java                 # ServerServiceDefinition + 演示端点
│   ├── demo02_http_interface/                  # HTTP Interface Client（文档 03）
│   │   ├── GitHubApi.java                      # @HttpExchange 接口定义
│   │   ├── HttpInterfaceConfig.java             # 客户端装配
│   │   └── HttpInterfaceClientDemo.java         # 演示端点
│   ├── demo03_virtual_threads/
│   │   └── VirtualThreadsDemo.java              # 虚拟线程并发对比（文档 04）
│   ├── demo04_aot_native/
│   │   └── AotNativeDemo.java                   # AOT / Native 说明与端点（文档 05）
│   ├── demo05_observability/                    # 可观测性（文档 06）
│   │   ├── ObservabilityDemo.java
│   │   └── CustomObservationConfig.java
│   ├── demo06_mvc_advanced/                     # MVC 进阶（文档 07）
│   │   ├── MvcAdvancedDemo.java
│   │   ├── config/WebMvcConfig.java
│   │   ├── controller/DemoController.java
│   │   ├── exception/                           # BusinessException / GlobalExceptionHandler
│   │   └── interceptor/PerformanceInterceptor.java
│   ├── demo07_data_jpa/                         # JPA 进阶（文档 08）
│   │   ├── JpaAdvancedDemo.java
│   │   ├── config/JpaConfig.java
│   │   ├── entity/                              # Article / Tag
│   │   ├── projection/ArticleSummary.java
│   │   ├── repository/ArticleRepository.java
│   │   └── spec/ArticleSpecifications.java
│   ├── demo08_events/                           # 事件驱动（文档 09）
│   │   ├── EventDrivenDemo.java
│   │   ├── config/EventMulticasterConfig.java
│   │   ├── event/                               # OrderCreatedEvent / OrderPaidEvent / OrderShippedEvent
│   │   ├── listener/                            # Order / Conditional / EventChain / Idempotent / Lifecycle
│   │   └── service/OrderService.java
│   ├── demo09_actuator/                         # Actuator 扩展（文档 10）
│   │   ├── CustomActuatorDemo.java
│   │   ├── endpoint/                            # AppInfoEndpoint / AppInfoEndpointWebExtension
│   │   └── health/ExternalApiHealthIndicator.java
│   └── demo10_security/                         # Security 6 进阶（文档 11）
│       ├── SecurityAdvancedDemo.java
│       ├── config/SecurityConfig.java
│       ├── controller/SecureController.java
│       ├── service/CustomUserDetailsService.java
│       └── util/JwtUtil.java
├── src/main/resources/application.yml
├── docs/                                        # VitePress 文档
└── pom.xml
```

> 注意：本项目**没有** `.proto` 文件。`GrpcServerDemo` 直接用 gRPC 原生 API 手动构建 `ServerServiceDefinition`，把 proto 定义以注释形式展示，避免为教学引入 protoc 编译链。

## 代码地图

| 包 | 关键类 | 职责 |
|----|--------|------|
| 根包 | [SpringAdvancedDemoApplication](../src/main/java/com/ibqy/spring/advanced/SpringAdvancedDemoApplication.java) | 启动入口 |
| `demo01_grpc` | [GrpcServerConfig](../src/main/java/com/ibqy/spring/advanced/demo01_grpc/GrpcServerConfig.java) / [GrpcServerDemo](../src/main/java/com/ibqy/spring/advanced/demo01_grpc/GrpcServerDemo.java) | 自动配置扩展点、`ServerServiceDefinition` 手写实现 |
| `demo02_http_interface` | [GitHubApi](../src/main/java/com/ibqy/spring/advanced/demo02_http_interface/GitHubApi.java) / [HttpInterfaceConfig](../src/main/java/com/ibqy/spring/advanced/demo02_http_interface/HttpInterfaceConfig.java) / [HttpInterfaceClientDemo](../src/main/java/com/ibqy/spring/advanced/demo02_http_interface/HttpInterfaceClientDemo.java) | `@HttpExchange` 声明式接口、代理装配、调用演示 |
| `demo03_virtual_threads` | [VirtualThreadsDemo](../src/main/java/com/ibqy/spring/advanced/demo03_virtual_threads/VirtualThreadsDemo.java) | 平台线程 vs 虚拟线程吞吐对比 |
| `demo04_aot_native` | [AotNativeDemo](../src/main/java/com/ibqy/spring/advanced/demo04_aot_native/AotNativeDemo.java) | AOT 处理与 Native Image 说明、运行时探测 |
| `demo05_observability` | [ObservabilityDemo](../src/main/java/com/ibqy/spring/advanced/demo05_observability/ObservabilityDemo.java) / [CustomObservationConfig](../src/main/java/com/ibqy/spring/advanced/demo05_observability/CustomObservationConfig.java) | 指标埋点、Observation 自定义 |
| `demo06_mvc_advanced` | [DemoController](../src/main/java/com/ibqy/spring/advanced/demo06_mvc_advanced/controller/DemoController.java) / [PerformanceInterceptor](../src/main/java/com/ibqy/spring/advanced/demo06_mvc_advanced/interceptor/PerformanceInterceptor.java) / [GlobalExceptionHandler](../src/main/java/com/ibqy/spring/advanced/demo06_mvc_advanced/exception/GlobalExceptionHandler.java) | 拦截器链、统一异常、SSE、消息转换 |
| `demo07_data_jpa` | [ArticleRepository](../src/main/java/com/ibqy/spring/advanced/demo07_data_jpa/repository/ArticleRepository.java) / [ArticleSpecifications](../src/main/java/com/ibqy/spring/advanced/demo07_data_jpa/spec/ArticleSpecifications.java) / [ArticleSummary](../src/main/java/com/ibqy/spring/advanced/demo07_data_jpa/projection/ArticleSummary.java) | Specification 动态查询、Projection、EntityGraph、Auditing |
| `demo08_events` | [OrderService](../src/main/java/com/ibqy/spring/advanced/demo08_events/service/OrderService.java) / [OrderEventListener](../src/main/java/com/ibqy/spring/advanced/demo08_events/listener/OrderEventListener.java) / [IdempotentOrderListener](../src/main/java/com/ibqy/spring/advanced/demo08_events/listener/IdempotentOrderListener.java) | 事务事件、事件链、条件监听、幂等 |
| `demo09_actuator` | [AppInfoEndpoint](../src/main/java/com/ibqy/spring/advanced/demo09_actuator/endpoint/AppInfoEndpoint.java) / [AppInfoEndpointWebExtension](../src/main/java/com/ibqy/spring/advanced/demo09_actuator/endpoint/AppInfoEndpointWebExtension.java) / [ExternalApiHealthIndicator](../src/main/java/com/ibqy/spring/advanced/demo09_actuator/health/ExternalApiHealthIndicator.java) | `@Endpoint` 自定义端点、Web 扩展、业务健康检查 |
| `demo10_security` | [SecurityConfig](../src/main/java/com/ibqy/spring/advanced/demo10_security/config/SecurityConfig.java) / [JwtUtil](../src/main/java/com/ibqy/spring/advanced/demo10_security/util/JwtUtil.java) / [CustomUserDetailsService](../src/main/java/com/ibqy/spring/advanced/demo10_security/service/CustomUserDetailsService.java) / [SecureController](../src/main/java/com/ibqy/spring/advanced/demo10_security/controller/SecureController.java) | Lambda DSL、JWT 签发校验、`@PreAuthorize` 方法安全 |

## 配置说明

核心配置项（`application.yml`）：

```yaml
server:
  port: 8080

spring:
  application:
    name: spring-advanced-demo
  threads:
    virtual:
      enabled: true                    # Demo03: 启用虚拟线程
  datasource:
    url: jdbc:h2:mem:advanced_demo     # 内存数据库
  jpa:
    hibernate:
      ddl-auto: create-drop            # 教学环境自动建表

management:                            # Demo05/09: Actuator 配置
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

grpc:
  server:
    port: 9090                         # Demo01: gRPC 端口
```

## 如何运行

### 1. 启动全部

```bash
mvn spring-boot:run
```

启动后可访问：
- Web 应用：`http://localhost:8080`
- H2 控制台：`http://localhost:8080/h2-console`
- Actuator：`http://localhost:8080/actuator`
- gRPC 服务：`localhost:9090`

### 2. 只学习某个 Demo

每个 Demo 位于独立包中（`demo01_grpc` ~ `demo10_security`），可按需查看代码和文档。

### 3. 构建原生镜像（Demo04）

```bash
# 需要 GraalVM JDK
mvn -Pnative native:compile
./target/spring-advanced-demo
```

## 学习路径建议

```
入门 → 进阶
  │
  ├─ 1. 整体架构（本文）
  ├─ 2. gRPC 服务端 → HTTP Interface Client（通信层）
  ├─ 3. Virtual Threads → AOT & Native Image（运行时优化）
  ├─ 4. 可观测性 → MVC 进阶 → JPA 进阶（Web & 数据）
  ├─ 5. 事件驱动 → Actuator 扩展（框架机制）
  └─ 6. Security 6 进阶（安全层，建议最后学习）
```

::: tip 建议
每个 Demo 都有对应的教学文档，建议按上表顺序学习。每个文档末尾都有 **面试考点** 和 **常见陷阱**，适合面试前复习。
:::
