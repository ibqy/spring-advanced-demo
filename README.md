<p align="center">
  <strong style="font-size: 28px;">&#x1F4DA; Spring Advanced Demo</strong><br>
  <span style="color: #656D76;">Spring Boot 4.1 + Spring Framework 7.0 高级特性教学 · 10 个高阶 Demo</span>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-2266EE?style=flat-square" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring_Boot-4.1.1-2266EE?style=flat-square" alt="Spring Boot 4.1.1">
  <img src="https://img.shields.io/badge/Spring_Framework-7.0.8-2266EE?style=flat-square" alt="Spring Framework 7.0.8">
  <img src="https://img.shields.io/github/license/ibqy/spring-advanced-demo?style=flat-square" alt="License">
  <img src="https://img.shields.io/github/stars/ibqy/spring-advanced-demo?style=flat-square" alt="Stars">
</p>

---

## &#x1F4D6; 简介

**Spring Advanced Demo** 是一个基于 **Spring Boot 4.1.1 + Spring Framework 7.0.8 + Java 21** 的高级特性教学项目。通过 **10 个独立 Demo**，覆盖从 gRPC 微服务通信到 Spring Security 6 安全的完整知识体系，帮助开发者从"会用 Spring"进阶到"理解 Spring"。

每个 Demo 均可独立运行，代码注释详尽，配套 [VitePress 文档站点](https://ibqy.github.io/spring-advanced-demo/) 提供深度讲解与面试考点。

## &#x1F6E0; 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 (LTS) | 虚拟线程、模式匹配、Records |
| Spring Boot | 4.1.1 | 应用框架核心 |
| Spring Framework | 7.0.8 | IoC / AOP / MVC / Security |
| Spring Data JPA | 4.1.x | Specification / Projection / Auditing |
| Spring Security | 6.4.x | Lambda DSL / JWT / 方法安全 |
| gRPC | 1.68.1 | 高性能 RPC 通信 |
| Micrometer + Prometheus | — | 指标采集与可观测性 |
| H2 Database | — | 内存数据库（教学演示） |
| Maven | — | 构建工具 |

## &#x1F3D7; 项目架构

```
spring-advanced-demo
│
├──  [基础设施层]
│   ├── Java 21 Runtime (Virtual Threads, Records, Pattern Matching)
│   ├── Spring Boot 4.1.1 Auto-Configuration
│   ├── H2 In-Memory Database
│   └── Maven Build
│
├──  [通信层]
│   ├── Demo01: gRPC Server ─────────── Protobuf 序列化 / 自动配置
│   └── Demo02: HTTP Interface Client ── 声明式 HTTP / WebClient
│
├──  [运行时优化层]
│   ├── Demo03: Virtual Threads ──────── 虚拟线程调度 / 结构化并发
│   └── Demo04: AOT & Native Image ───── 编译时优化 / GraalVM
│
├──  [Web & 数据层]
│   ├── Demo05: Observability ────────── Micrometer / OpenTelemetry
│   ├── Demo06: MVC Advanced ─────────── 拦截器 / SSE / 消息转换器
│   └── Demo07: Data JPA Advanced ────── Specification / EntityGraph
│
├──  [框架机制层]
│   ├── Demo08: Event-Driven ─────────── ApplicationEvent / @TransactionalEventListener
│   └── Demo09: Actuator Extensions ──── @Endpoint / HealthIndicator
│
└──  [安全层]
    └── Demo10: Security 6 ───────────── Lambda DSL / JWT / @PreAuthorize
```

## &#x1F4CB; 模块一览表

| # | Demo 名称 | 核心特性 | 关键注解 / API |
|---|-----------|---------|----------------|
| 01 | gRPC 服务端 | Spring Boot 4.1 自动配置 gRPC Server | `@GrpcService`, `GrpcServerFactory` |
| 02 | HTTP Interface Client | 声明式 HTTP 客户端（类似 Feign） | `@HttpExchange`, `@GetExchange` |
| 03 | Virtual Threads | Java 21 虚拟线程集成 | `spring.threads.virtual.enabled=true` |
| 04 | AOT & Native Image | 编译时优化与 GraalVM 原生镜像 | `@RegisterReflectionForBinding` |
| 05 | 可观测性 Observability | Micrometer + Prometheus 指标 | `@Timed`, `Counter`, `Timer` |
| 06 | Spring MVC 进阶 | 拦截器 / 全局异常 / SSE / 消息转换器 | `HandlerInterceptor`, `SseEmitter` |
| 07 | Spring Data JPA 进阶 | Specification / Projection / EntityGraph | `@EntityGraph`, `JpaSpecificationExecutor` |
| 08 | 事件驱动机制 | ApplicationEvent / 事务事件监听 | `@EventListener`, `@TransactionalEventListener` |
| 09 | 自定义 Actuator 端点 | 自定义 `@Endpoint` + HealthIndicator | `@Endpoint`, `HealthIndicator` |
| 10 | Spring Security 6 进阶 | Lambda DSL / JWT / 方法安全 | `@PreAuthorize`, `JwtAuthenticationConverter` |

## &#x1F680; 快速开始

### 环境要求

- **JDK** 21+
- **Maven** 3.9+
- **IDE**: IntelliJ IDEA 2024.1+（推荐）

### 克隆与运行

```bash
# 克隆仓库
git clone https://github.com/ibqy/spring-advanced-demo.git
cd spring-advanced-demo

# 编译
mvn clean compile

# 运行全部 Demo
mvn spring-boot:run

# 访问 H2 控制台
open http://localhost:8080/h2-console   # JDBC URL: jdbc:h2:mem:advanced_demo

# gRPC 服务端口
# localhost:9090
```

### 构建原生镜像（Demo04）

```bash
# 需要先安装 GraalVM
mvn -Pnative native:compile
./target/spring-advanced-demo
```

## &#x1F4D6; 文档站点

完整的 VitePress 教学文档：[https://ibqy.github.io/spring-advanced-demo/](https://ibqy.github.io/spring-advanced-demo/)

本地预览：

```bash
cd docs
npm install
npm run docs:dev
```

## &#x1F468;&#x200D;&#x1F4BB; 作者

**ibqy** &mdash; [GitHub](https://github.com/ibqy) &middot; [个人站](https://ibqy.github.io)

> 如果你也在学习 Spring 的高级特性，欢迎 Star 或提交 Issue 一起讨论。

## &#x1F4DC; License

MIT &copy; ibqy
