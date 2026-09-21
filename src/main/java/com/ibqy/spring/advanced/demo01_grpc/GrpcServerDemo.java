package com.ibqy.spring.advanced.demo01_grpc;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

/**
 * ============================================================
 * Demo 01: gRPC 服务端演示 - 模拟 gRPC 服务实现
 * ============================================================
 *
 * 【背景知识】
 * gRPC 是 Google 开源的高性能 RPC 框架，基于 HTTP/2 + Protocol Buffers。
 * Spring Boot 4.1 对 gRPC 提供了更好的自动配置支持：
 *   - 自动检测 @GrpcService 注解的 Bean
 *   - 自动注册到 gRPC ServerBuilder
 *   - 通过 application.yml 配置端口、拦截器等
 *
 * 【本 Demo 说明】
 * 由于没有 .proto 编译生成的代码，本 Demo 使用 gRPC 原生 API
 * 手动构建 ServerServiceDefinition，模拟 proto 编译后的效果。
 * 实际项目中，你应该使用 protobuf-maven-plugin 自动生成代码。
 *
 * 【核心注解】
 * - @GrpcService：标注在 BindableService 实现类上，Spring 自动注册到 gRPC Server
 * - 等价于手动调用 ServerBuilder.addService()
 *
 * 【面试考点】
 * 1. gRPC vs REST：gRPC 使用二进制协议(Protobuf)，比 JSON 小 3-10 倍，延迟更低
 * 2. gRPC 四种调用模式：Unary / Server Streaming / Client Streaming / Bidirectional Streaming
 * 3. Spring Boot 4.1 中 gRPC 自动配置的原理：GrpcServerAutoConfiguration 扫描 @GrpcService
 * 4. gRPC 拦截器(Interceptor)机制：类似 Servlet Filter，可做认证、日志、限流
 *
 * @author ibqy
 * @since 2026-09-21
 */
public class GrpcServerDemo {

    /**
     * 运行 gRPC 服务端演示
     * 由于 gRPC 服务会与 Spring Boot 应用同时启动（占用 9090 端口），
     * 这里只演示服务构建过程，不实际启动 Server，避免端口冲突。
     */
    public void run() {
        System.out.println("""
                
                ====================================================
                 Demo 01: gRPC 服务端演示
                ====================================================
                """);

        // =============================================
        // 第一部分：展示 BindableService 的实现方式
        // =============================================
        System.out.println("[1] 模拟 gRPC 服务定义");
        System.out.println("--------------------------------------------");

        // 创建一个模拟的 BindableService
        // 在实际项目中，这个类是由 protoc 编译器根据 .proto 文件自动生成的
        // 例如：public class UserServiceGrpc.UserServiceImplBase extends BindableService
        BindableService mockUserService = new MockUserService();

        // BindableService 的核心方法：bindService()
        // 返回 ServerServiceDefinition，定义了服务名、方法、序列化方式等
        ServerServiceDefinition serviceDef = mockUserService.bindService();

        System.out.println("服务名称: " + serviceDef.getServiceDescriptor().getName());
        System.out.println("服务包含的方法数: " + serviceDef.getMethods().size());

        // 打印所有方法信息
        serviceDef.getMethods().forEach(method -> {
            System.out.println("  - 方法: " + method.getMethodDescriptor().getFullMethodName());
            System.out.println("    类型: " + method.getMethodDescriptor().getType().name());
        });

        // =============================================
        // 第二部分：展示 gRPC 自动配置原理
        // =============================================
        System.out.println("""
                
                [2] Spring Boot 4.1 gRPC 自动配置原理
                --------------------------------------------
                在 Spring Boot 4.1 中，gRPC 自动配置流程如下：
                
                1. GrpcServerAutoConfiguration 自动装配
                   └── 读取 grpc.server.port 配置（默认 9090）
                   └── 创建 ServerBuilder（NettyServerBuilder）
                   └── 配置 SSL/TLS、最大消息大小等
                
                2. GrpcServiceBeanPostProcessor 处理 @GrpcService
                   └── 扫描所有标注 @GrpcService 的 Bean
                   └── 调用 serverBuilder.addService(bean)
                   └── 自动注册 GrpcServerInterceptor
                
                3. GrpcServerLifecycle 管理生命周期
                   └── 实现 SmartLifecycle 接口
                   └── 应用启动时 server.start()
                   └── 应用关闭时 server.shutdown()
                """);

        // =============================================
        // 第三部分：展示 .proto 文件示例（注释形式）
        // =============================================
        System.out.println("""
                [3] 典型的 .proto 文件定义（教学示例）
                --------------------------------------------
                // user_service.proto
                syntax = "proto3";
                package com.ibqy.spring.advanced.demo01_grpc;
                
                option java_multiple_files = true;
                option java_package = "com.ibqy.spring.advanced.demo01_grpc.proto";
                
                // 服务定义
                service UserService {
                  // 一元调用：根据 ID 查询用户
                  rpc GetUser (GetUserRequest) returns (UserResponse);
                  
                  // 服务端流式：获取所有用户列表
                  rpc ListUsers (ListUsersRequest) returns (stream UserResponse);
                  
                  // 客户端流式：批量创建用户
                  rpc CreateUsers (stream CreateUserRequest) returns (CreateUsersResponse);
                  
                  // 双向流式：实时聊天
                  rpc Chat (stream ChatMessage) returns (stream ChatMessage);
                }
                
                // 消息定义
                message GetUserRequest {
                  int64 user_id = 1;
                }
                
                message UserResponse {
                  int64 user_id = 1;
                  string name = 2;
                  string email = 3;
                }
                """);

        // =============================================
        // 第四部分：gRPC vs REST 对比
        // =============================================
        System.out.println("""
                [4] gRPC vs REST 对比（面试高频题）
                --------------------------------------------
                | 特性           | gRPC                     | REST                  |
                |---------------|--------------------------|-----------------------|
                | 协议           | HTTP/2                   | HTTP/1.1 (通常)        |
                | 数据格式       | Protobuf (二进制)         | JSON (文本)            |
                | 性能           | 高（序列化快，体积小）     | 一般                   |
                | 流式支持       | 原生四种模式              | 仅 SSE/WebSocket       |
                | 浏览器支持     | 需 grpc-web 转换          | 原生支持               |
                | 代码生成       | 自动生成客户端/服务端      | 手动编写或 Swagger       |
                | 适用场景       | 微服务内部通信             | 对外 API               |
                | 负载均衡       | 需 L7 LB（如 Envoy）      | 传统 L4 LB 即可         |
                
                【面试考点】什么时候用 gRPC？什么时候用 REST？
                - 微服务间内部通信 -> gRPC（高性能、强类型、自动代码生成）
                - 对外提供 API -> REST（通用性好、浏览器友好、调试方便）
                - 流式数据 -> gRPC（原生双向流支持）
                - 浏览器直接调用 -> REST（gRPC-Web 是变通方案）
                """);

        // =============================================
        // 第五部分：配置示例
        // =============================================
        System.out.println("""
                [5] Spring Boot gRPC 配置示例 (application.yml)
                --------------------------------------------
                # gRPC 服务端配置
                grpc:
                  server:
                    port: 9090                    # gRPC 服务端口
                    security:
                      enabled: false              # 是否启用 TLS
                    # 拦截器配置
                    enable-keep-alive: true       # 开启 HTTP/2 Keep-Alive
                    keep-alive-time: 30s
                
                # 对应 Java 配置类见 GrpcServerConfig.java
                """);

        System.out.println("[Demo 01 完成] gRPC 服务端演示结束");
        System.out.println("====================================================");
    }

    /**
     * 模拟的 gRPC 服务实现
     * 在实际项目中，这个类会继承由 protoc 生成的 XxxServiceGrpc.XxxServiceImplBase
     * 然后标注 @GrpcService 注解让 Spring 自动注册
     *
     * 这里手动实现 BindableService 接口来模拟生成代码的效果
     */
    static class MockUserService implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder("com.ibqy.UserService")
                    .addMethod(
                            io.grpc.MethodDescriptor.<byte[], byte[]>newBuilder()
                                    .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName("com.ibqy.UserService/GetUser")
                                    .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                                    .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                                    .build(),
                            io.grpc.stub.ServerCalls.asyncUnaryCall(
                                    new io.grpc.stub.ServerCalls.UnaryMethod<byte[], byte[]>() {
                                        @Override
                                        public void invoke(byte[] request, StreamObserver<byte[]> responseObserver) {
                                            System.out.println("收到 GetUser 请求");
                                            responseObserver.onCompleted();
                                        }
                                    }
                            )
                    )
                    .build();
        }
    }

    /**
     * 简单的字节数组 Marshaller，用于演示
     * 实际项目中由 Protobuf 自动生成
     */
    static class ByteArrayMarshaller implements io.grpc.MethodDescriptor.Marshaller<byte[]> {
        static final ByteArrayMarshaller INSTANCE = new ByteArrayMarshaller();

        @Override
        public java.io.InputStream stream(byte[] value) {
            return new java.io.ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(java.io.InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
