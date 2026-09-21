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
├── src/
│   └── main/
│       ├── java/com/ibqy/spring/advanced/
│       │   ├── SpringAdvancedDemoApplication.java    # 启动类
│       │   │
│       │   ├── demo01/grpc/                          # gRPC 服务端
│       │   │   ├── config/                           # gRPC 自动配置
│       │   │   ├── service/                          # gRPC Service 实现
│       │   │   └── interceptor/                      # gRPC 拦截器
│       │   │
│       │   ├── demo02/httpclient/                    # HTTP Interface Client
│       │   │   ├── client/                           # @HttpExchange 接口
│       │   │   ├── config/                           # WebClient 配置
│       │   │   └── controller/                       # 演示控制器
│       │   │
│       │   ├── demo03/virtualthreads/                # Virtual Threads
│       │   │   ├── controller/                       # 并发演示
│       │   │   ├── service/                          # 虚拟线程服务
│       │   │   └── config/                           # 线程池配置
│       │   │
│       │   ├── demo04/aot/                           # AOT & Native Image
│       │   │   ├── config/                           # AOT 注册
│       │   │   └── runtime/                          # 运行时提示
│       │   │
│       │   ├── demo05/observability/                 # 可观测性
│       │   │   ├── config/                           # Micrometer 配置
│       │   │   ├── service/                          # 指标埋点
│       │   │   └── controller/                       # 演示端点
│       │   │
│       │   ├── demo06/mvc/                           # Spring MVC 进阶
│       │   │   ├── config/                           # MVC 配置
│       │   │   ├── controller/                       # 控制器示例
│       │   │   ├── exception/                        # 全局异常处理
│       │   │   ├── interceptor/                      # 拦截器
│       │   │   └── converter/                        # 消息转换器
│       │   │
│       │   ├── demo07/jpa/                           # Spring Data JPA 进阶
│       │   │   ├── entity/                           # 实体类
│       │   │   ├── repository/                       # Repository 接口
│       │   │   ├── service/                          # 查询服务
│       │   │   └── config/                           # Auditing 配置
│       │   │
│       │   ├── demo08/event/                         # 事件驱动
│       │   │   ├── event/                            # 事件定义
│       │   │   ├── listener/                         # 事件监听器
│       │   │   └── publisher/                        # 事件发布器
│       │   │
│       │   ├── demo09/actuator/                      # 自定义 Actuator
│       │   │   ├── endpoint/                         # 自定义端点
│       │   │   └── health/                           # 健康指示器
│       │   │
│       │   └── demo10/security/                      # Spring Security 6
│       │       ├── config/                           # Security 配置
│       │       ├── jwt/                              # JWT 工具
│       │       ├── controller/                       # 安全控制器
│       │       └── model/                            # 用户模型
│       │
│       └── resources/
│           ├── application.yml                       # 主配置
│           └── proto/                                # Protobuf 定义
│               └── demo01/
│                   └── greeting.proto
│
├── docs/                                             # VitePress 文档
│   ├── .vitepress/
│   │   └── config.mts
│   ├── index.md
│   └── *.md
│
├── pom.xml
└── README.md
```

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

每个 Demo 位于独立包中（`demo01` ~ `demo10`），可按需查看代码和文档。

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
