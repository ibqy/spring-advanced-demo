# 02 · gRPC 自动配置

> Spring Boot 4.1 如何用自动配置消除 gRPC 服务端的样板代码？

## 概述

gRPC 是 Google 开源的高性能 RPC 框架，基于 HTTP/2 和 Protobuf。传统方式搭建 gRPC Server 需要手动创建 `ServerBuilder`、注册服务、管理生命周期。本 Demo 演示如何利用 **Spring Boot 4.1 自动配置** 将 gRPC Server 的启动简化为零样板代码。

## 核心概念

### gRPC 的四种服务类型

| 类型 | 特点 | 适用场景 |
|------|------|---------|
| **Unary** | 单次请求 → 单次响应 | 简单查询 |
| **Server Streaming** | 单次请求 → 流式响应 | 数据推送 |
| **Client Streaming** | 流式请求 → 单次响应 | 批量上传 |
| **Bidirectional Streaming** | 双向流式通信 | 实时聊天 |

### Protobuf 消息定义

```protobuf
// proto/demo01/greeting.proto
syntax = "proto3";

package demo01;

option java_multiple_files = true;
option java_package = "com.ibqy.spring.advanced.demo01.grpc.proto";

service GreetingService {
  rpc SayHello (HelloRequest) returns (HelloResponse);           // Unary
  rpc StreamGreetings (HelloRequest) returns (stream HelloResponse); // Server Streaming
}

message HelloRequest {
  string name = 1;
}

message HelloResponse {
  string message = 1;
  int64 timestamp = 2;
}
```

## 代码走读

### 1. 自动配置原理

Spring Boot 通过 `grpc-server-spring-boot-starter` 实现自动配置：

```java
// 自动配置类（starter 内部）
@AutoConfiguration
@ConditionalOnClass(GRpcServerBuilder.class)
@EnableConfigurationProperties(GRpcServerProperties.class)
public class GrpcServerAutoConfiguration {

    @Bean
    public GrpcServerRunner grpcServerRunner(
            GrpcServerFactory factory,
            List<BindableService> services) {
        return new GrpcServerRunner(factory, services);
    }
}
```

**自动配置做了什么？**

1. 扫描所有 `@GrpcService` 注解的 Bean
2. 自动注册到 `ServerBuilder`
3. 绑定到配置的端口（`grpc.server.port=9090`）
4. 管理 Server 的生命周期（`SmartLifecycle`）

### 2. gRPC Service 实现

```java
// demo01/grpc/service/GreetingServiceImpl.java
@GrpcService
public class GreetingServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String message = "Hello, " + request.getName() + "!";
        HelloResponse response = HelloResponse.newBuilder()
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

::: tip 关键注解
`@GrpcService` 是 starter 提供的注解，Spring Boot 启动时会自动扫描并将该类注册为 gRPC 服务。无需手动构建 Server 实例。
:::

### 3. gRPC 拦截器

```java
// demo01/grpc/interceptor/LoggingInterceptor.java
@GrpcGlobalServerInterceptor
public class LoggingInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        log.info("gRPC call: {}", methodName);

        long start = System.currentTimeMillis();
        ForwardingServerCall.SimpleForwardingServerCall<RespT> wrappedCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        long duration = System.currentTimeMillis() - start;
                        log.info("gRPC {} completed in {}ms, status: {}", methodName, duration, status);
                        super.close(status, trailers);
                    }
                };

        return next.startCall(wrappedCall, headers);
    }
}
```

## 注解与 API 参考

| 注解 / API | 说明 |
|-----------|------|
| `@GrpcService` | 标记一个类为 gRPC 服务实现，自动注册到 Server |
| `@GrpcGlobalServerInterceptor` | 全局 gRPC 服务端拦截器 |
| `StreamObserver<T>` | gRPC 流式观察者，用于发送响应/请求 |
| `ServerCall` | 代表一次 RPC 调用 |
| `Metadata` | gRPC 请求/响应头信息 |
| `Status` | RPC 调用状态码（OK, CANCELLED, INTERNAL 等） |

## 面试考点

::: warning 高频面试题
1. **gRPC 与 REST 的区别是什么？**
   - gRPC 基于 HTTP/2，支持多路复用、双向流；REST 基于 HTTP/1.1
   - gRPC 使用 Protobuf 二进制序列化，体积更小、速度更快
   - gRPC 有强类型契约（`.proto` 文件），REST 依赖文档

2. **Spring Boot 如何自动配置 gRPC Server？**
   - 通过 `@EnableAutoConfiguration` 加载 starter 中的自动配置类
   - 自动扫描 `@GrpcService` Bean → 注册到 `ServerBuilder` → 绑定端口 → 生命周期管理

3. **gRPC 拦截器和 Spring MVC 拦截器的区别？**
   - gRPC 拦截器工作在 HTTP/2 层，处理的是 `ServerCall`
   - Spring MVC 拦截器工作在 Servlet 层，处理的是 `HttpServletRequest/Response`
   - 两者互不干扰，可以在同一个应用中并存
:::

## 常见陷阱

::: danger 陷阱 1：端口冲突
gRPC 默认端口 9090，确保不要与 Web 端口（8080）冲突。在 `application.yml` 中明确配置：
```yaml
grpc:
  server:
    port: 9090
```
:::

::: danger 陷阱 2：Protobuf 编译遗漏
`.proto` 文件需要 `protobuf-maven-plugin` 编译为 Java 类。确保 pom.xml 中配置了插件：
```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
</plugin>
```
:::

::: danger 陷阱 3：StreamObserver 未调用 onCompleted
Server Streaming 模式下，如果忘记调用 `responseObserver.onCompleted()`，客户端会一直等待。务必在流结束时调用。
:::

## 延伸阅读

- [gRPC Java 官方文档](https://grpc.io/docs/languages/java/)
- [Spring Boot gRPC Starter](https://github.com/LogiStic/grpc-spring-boot-starter)
- [Protocol Buffers 语言指南](https://protobuf.dev/programming-guides/proto3/)
- [HTTP/2 协议详解](https://http2-explained.haxx.se/)
