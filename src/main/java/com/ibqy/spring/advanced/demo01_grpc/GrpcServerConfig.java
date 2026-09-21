package com.ibqy.spring.advanced.demo01_grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * Demo 01 配置: gRPC 服务端配置类
 * ============================================================
 *
 * 【核心职责】
 * 展示如何在 Spring Boot 中手动配置 gRPC Server（理解自动配置背后的原理）。
 *
 * 【Spring Boot 4.1 自动配置 vs 手动配置】
 * Spring Boot 4.1 提供了 gRPC 自动配置（通过 net.devh:grpc-server-spring-boot-starter），
 * 大多数情况下只需标注 @GrpcService 即可。但理解手动配置有助于：
 *   1. 面试中解释自动配置原理
 *   2. 需要自定义高级配置（如自定义 SSL、连接池等）
 *   3. 排查自动配置不生效的问题
 *
 * 【面试考点】
 * 1. @ConditionalOnProperty 的作用：仅在配置了 grpc.server.port 时才生效
 * 2. @Bean(destroyMethod = "shutdown") 的生命周期管理
 * 3. ServerInterceptor 拦截器的链式调用机制
 * 4. gRPC 的线程模型：Boss/Worker 线程池
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Configuration
public class GrpcServerConfig {

    /**
     * gRPC 服务端口，默认 9090
     * 对应配置: grpc.server.port=9090
     */
    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    /**
     * Boss 线程数（负责接受新连接）
     * 通常 1 个就够了，类似 Netty 的 BossGroup
     */
    @Value("${grpc.server.boss-threads:1}")
    private int bossThreads;

    /**
     * Worker 线程数（负责处理请求）
     * 默认 0 表示使用 gRPC 内部默认值（CPU 核心数的 2 倍）
     */
    @Value("${grpc.server.worker-threads:0}")
    private int workerThreads;

    /**
     * 创建 gRPC Server Bean
     *
     * 【注意】这里使用 @ConditionalOnProperty 避免与教学环境冲突
     * 在真实项目中，你会移除此条件，让 gRPC Server 随应用启动
     *
     * 【自动配置原理】
     * net.devh 的 GrpcServerAutoConfiguration 做了以下事情：
     *   1. 创建 NettyServerBuilder
     *   2. 配置端口、线程池、SSL
     *   3. 扫描 @GrpcService Bean 并 addService()
     *   4. 注册 ServerInterceptor
     *   5. 包装成 GrpcServerLifecycle 管理生命周期
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "grpc.server.enabled", havingValue = "true", matchIfMissing = false)
    public Server grpcServer(LoggingInterceptor loggingInterceptor) throws IOException {
        // 1. 构建 ServerBuilder（类似 ServerBootstrap）
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(grpcPort);

        // 2. 配置线程池
        //    bossThread: 负责接受新连接（类似 Reactor 的 Accept 线程）
        //    workerThread: 负责处理 I/O 读写（类似 Reactor 的 IO 线程）
        //    默认使用 ExecutorService，也可以自定义
        if (workerThreads > 0) {
            serverBuilder = serverBuilder
                    .executor(java.util.concurrent.Executors.newFixedThreadPool(workerThreads));
        }

        // 3. 添加服务（等同于 @GrpcService 的效果）
        //    在实际项目中，这里会自动扫描所有 @GrpcService 标注的 Bean
        GrpcServerDemo.MockUserService mockUserService = new GrpcServerDemo.MockUserService();
        serverBuilder.addService(
                // ServerInterceptors.intercept() 可以在服务上添加拦截器
                // 拦截器按添加顺序链式调用（先添加的先执行）
                ServerInterceptors.intercept(
                        mockUserService,
                        loggingInterceptor
                )
        );

        // 4. 配置最大消息大小（默认 4MB）
        serverBuilder
                .maxInboundMessageSize(10 * 1024 * 1024)  // 10MB
                .maxInboundMetadataSize(8192);              // 8KB 元数据

        // 5. 构建并启动 Server
        Server server = serverBuilder.build();
        server.start();
        System.out.println("[gRPC] Server started on port " + grpcPort);

        return server;
    }

    /**
     * 创建日志拦截器 Bean
     * gRPC 拦截器类似 Servlet Filter，可以在请求/响应的各个阶段介入
     */
    @Bean
    public LoggingInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    /**
     * gRPC 日志拦截器
     *
     * 【拦截器执行顺序】
     * 请求进入时：intercept() -> call.listen() -> handler.invoke()
     * 响应返回时：call.sendMessage() -> call.close()
     *
     * 【面试考点】
     * gRPC 拦截器 vs Servlet Filter 的区别：
     * - gRPC 基于 HTTP/2 的 Stream 模型，拦截器是异步的
     * - 可以拦截 Streaming 的每条消息（ServerCall.Listener）
     * - 支持 ForwardingServerCall 包装响应
     */
    public static class LoggingInterceptor implements ServerInterceptor {

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {

            // 记录请求信息
            String methodName = call.getMethodDescriptor().getFullMethodName();
            long startTime = System.nanoTime();
            System.out.println("[gRPC Interceptor] 收到请求: " + methodName);

            // 使用 ForwardingServerCall 包装原始 call，拦截响应
            ServerCall<ReqT, RespT> wrappedCall = new io.grpc.ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                @Override
                public void close(io.grpc.Status status, Metadata trailers) {
                    // 拦截响应，记录耗时
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                    System.out.println("[gRPC Interceptor] 响应完成: " + methodName
                            + " | 状态: " + status.getCode()
                            + " | 耗时: " + durationMs + "ms");
                    super.close(status, trailers);
                }
            };

            // 继续拦截器链（调用下一个拦截器或最终的服务方法）
            return next.startCall(wrappedCall, headers);
        }
    }
}
