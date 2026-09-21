---
layout: home

hero:
  name: Spring Advanced Demo
  text: Spring Boot 4.1 + Spring Framework 7.0
  tagline: 10 个高阶 Demo，从"会用"到"理解" Spring 的完整进阶之路
  actions:
    - theme: brand
      text: 开始阅读
      link: /01-architecture
    - theme: alt
      text: GitHub
      link: https://github.com/ibqy/spring-advanced-demo

features:
  - icon: "&#x1F4E1;"
    title: "01 · gRPC 服务端"
    details: "Spring Boot 4.1 自动配置 gRPC Server，Protobuf 序列化，零样板代码启动高性能 RPC 服务。"
    link: /02-gRPC自动配置
  - icon: "&#x1F310;"
    title: "02 · HTTP Interface Client"
    details: "声明式 HTTP 客户端，类似 Feign 但更轻量。@HttpExchange 注解定义接口，Spring 自动实现。"
    link: /03-HTTP-Interface-Client
  - icon: "&#x1F9F5;"
    title: "03 · Virtual Threads"
    details: "Java 21 虚拟线程集成，一行配置启用，高并发场景下资源占用降低 10 倍。"
    link: /04-Virtual-Threads
  - icon: "&#x26A1;"
    title: "04 · AOT & Native Image"
    details: "编译时优化与 GraalVM 原生镜像，毫秒级启动、极低内存占用，云原生部署利器。"
    link: /05-AOT-Native-Image
  - icon: "&#x1F4CA;"
    title: "05 · 可观测性 Observability"
    details: "Micrometer + OpenTelemetry 全链路可观测，指标、追踪、日志三位一体。"
    link: /06-可观测性
  - icon: "&#x1F3A8;"
    title: "06 · Spring MVC 进阶"
    details: "拦截器、全局异常处理、SSE 推送、自定义消息转换器，打造企业级 Web 层。"
    link: /07-MVC进阶
  - icon: "&#x1F4BE;"
    title: "07 · Spring Data JPA 进阶"
    details: "Specification 动态查询、Projection 视图映射、EntityGraph 抓取优化、Auditing 审计。"
    link: /08-Data-JPA进阶
  - icon: "&#x1F514;"
    title: "08 · 事件驱动机制"
    details: "ApplicationEvent 发布订阅、@TransactionalEventListener 事务事件，解耦业务逻辑。"
    link: /09-事件驱动
  - icon: "&#x1F3E5;"
    title: "09 · 自定义 Actuator 端点"
    details: "自定义 @Endpoint 扩展监控端点、HealthIndicator 健康检查，运维友好设计。"
    link: /10-Actuator扩展
  - icon: "&#x1F6E1;"
    title: "10 · Spring Security 6 进阶"
    details: "Lambda DSL 配置、JWT 无状态认证、@PreAuthorize 方法安全，构建零信任架构。"
    link: /11-Security6进阶
---

## &#x1F680; 快速开始

::: code-group

```bash [克隆项目]
git clone https://github.com/ibqy/spring-advanced-demo.git
cd spring-advanced-demo
```

```bash [编译运行]
mvn clean compile
mvn spring-boot:run
```

```yaml [访问端点]
# Web 服务
http://localhost:8080

# H2 控制台
http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:advanced_demo

# gRPC 服务
grpc://localhost:9090

# Actuator 监控
http://localhost:8080/actuator
```

:::

## &#x1F4CB; 技术栈一览

| 技术 | 版本 | 核心用途 |
|------|------|---------|
| Java | 21 (LTS) | 虚拟线程 · Records · 模式匹配 |
| Spring Boot | 4.1.1 | 应用框架 · 自动配置 |
| Spring Framework | 7.0.8 | IoC · AOP · MVC · Security |
| Spring Data JPA | 4.1.x | Specification · Projection · Auditing |
| Spring Security | 6.4.x | Lambda DSL · JWT · 方法安全 |
| gRPC | 1.68.1 | 高性能 RPC |
| Micrometer | — | 指标采集 · Prometheus |
| H2 | — | 内存数据库（教学演示） |
