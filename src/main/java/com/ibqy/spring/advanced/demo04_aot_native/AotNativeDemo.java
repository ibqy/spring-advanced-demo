package com.ibqy.spring.advanced.demo04_aot_native;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

/**
 * ============================================================
 * Demo 04: AOT (Ahead-of-Time) & Native Image 演示
 * ============================================================
 *
 * 【背景知识】
 * AOT（提前编译）是将 Java 应用编译为原生可执行文件的技术。
 * Spring Boot 3.0 引入 GraalVM Native Image 支持，
 * Spring Boot 4.1 进一步优化了 AOT 处理流程。
 *
 * 核心概念：
 * - AOT 处理阶段：在编译时（而非运行时）分析并优化 Bean 定义
 * - GraalVM Native Image：将 JVM 应用编译为平台原生可执行文件
 * - RuntimeHints API：告诉 Native Image 哪些反射/代理/资源需要保留
 *
 * 【为什么要 AOT？】
 * - 启动时间：JVM ~2s -> Native ~50ms（40倍提升）
 * - 内存占用：JVM ~200MB -> Native ~50MB（4倍降低）
 * - 峰值性能：Native 可能更快（JIT 优化更激进）
 *
 * 【代价】
 * - 编译时间长：mvn -Pnative native:compile ~5-10分钟
 * - 调试困难：没有 JVM 调试器支持（在改善中）
 * - 动态特性受限：反射、动态代理需要额外配置
 *
 * 【面试考点】
 * 1. AOT 和 JIT 的区别：编译时机不同（编译时 vs 运行时）
 * 2. RuntimeHints 的作用：解决 Native Image 的反射/动态代理问题
 * 3. Spring AOT 处理流程：BeanFactory -> BeanDefinition -> AOT 优化
 * 4. Native Image 的限制：不支持运行时类加载、有限反射支持
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
public class AotNativeDemo {

    /**
     * 应用启动后运行演示
     */
    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        System.out.println("""
                
                ====================================================
                 Demo 04: AOT & Native Image 演示
                ====================================================
                """);

        demoAotProcessingPhase();
        demoRuntimeHintsApi();
        demoReflectionRegistration();
        demoNativeImageCommands();
        demoComparison();
        demoInterviewTips();

        System.out.println("[Demo 04 完成] AOT & Native Image 演示结束");
        System.out.println("====================================================");
    }

    /**
     * 演示 1: AOT 处理阶段说明
     */
    private void demoAotProcessingPhase() {
        System.out.println("""
                [1] AOT 处理阶段解析
                --------------------------------------------
                
                Spring Boot AOT 处理分为两个阶段：
                
                === 编译时 AOT 处理（Build Time） ===
                
                mvn spring-boot:process-aot
                
                这个阶段做的事情：
                1. 启动 ApplicationContext（但不启动 Web Server）
                2. 扫描所有 @Configuration / @Bean / @Component
                3. 生成 Bean 定义的静态代码（避免运行时反射）
                4. 处理 @Conditional 条件注解（确定哪些 Bean 存在）
                5. 生成 RuntimeHints（反射、代理、资源访问提示）
                6. 输出到 target/aot-generated/ 目录
                
                生成的关键文件：
                - ApplicationContextInitializer 实现类
                - BeanDefinitionRegistrar 实现类
                - RuntimeHints 注册代码
                
                === 编译为 Native Image ===
                
                mvn -Pnative native:compile
                # 或
                mvn spring-boot:build-image -Pnative
                
                这个阶段做的事情：
                1. 调用 GraalVM native-image 编译器
                2. 基于 AOT 生成的代码和 hints 进行编译
                3. 执行静态分析，去除未使用的代码（Dead Code Elimination）
                4. 生成平台原生可执行文件（ELF/Mach-O/PE）
                5. 输出 target/application
                """);
    }

    /**
     * 演示 2: RuntimeHints API 使用
     *
     * RuntimeHints 是 Spring 6.0 引入的 API，用于告诉 AOT 处理器
     * 哪些运行时特性需要保留（因为 Native Image 默认不支持动态特性）。
     */
    private void demoRuntimeHintsApi() {
        System.out.println("""
                [2] RuntimeHints API 使用演示
                --------------------------------------------
                
                RuntimeHints 支持注册以下类型的提示：
                
                1. 反射提示（Reflection）
                   - 哪些类需要反射访问
                   - 哪些方法需要运行时调用
                   - 哪些字段需要运行时读取
                
                2. 代理提示（Proxy）
                   - 哪些接口需要 JDK 动态代理
                   - 哪些类需要 CGLIB 代理
                
                3. 资源提示（Resource）
                   - 哪些文件需要打包到 Native Image
                   - classpath 资源的访问模式
                
                4. JNI 提示（JNI）
                   - 哪些 native 方法需要保留
                
                5. 序列化提示（Serialization）
                   - 哪些类需要序列化支持
                
                === 代码示例 ===
                \s""" + """
                // 创建 RuntimeHints 并注册提示
                RuntimeHints hints = new RuntimeHints();
                
                // 1. 注册反射提示
                hints.reflection()
                    .registerType(MyDto.class, MemberCategory.values())
                    .registerType(MyService.class, 
                        MemberCategory.INVOKE_DECLARED_METHODS);
                
                // 2. 注册代理提示
                hints.proxies()
                    .registerJdkProxy(MyInterface.class);
                
                // 3. 注册资源提示
                hints.resources()
                    .registerPattern("config/*.yml")
                    .registerPattern("templates/**");
                
                // 4. 注册序列化提示
                hints.serialization()
                    .registerType(MyDto.class);
                """);

        // 实际创建 RuntimeHints 演示
        System.out.println("\n  [实际演示] 创建 RuntimeHints:");
        RuntimeHints hints = new RuntimeHints();

        // 注册反射提示
        hints.reflection()
                .registerType(String.class, MemberCategory.DECLARED_FIELDS)
                .registerType(AotNativeDemo.class, MemberCategory.INVOKE_DECLARED_METHODS);

        // 注册资源提示
        hints.resources()
                .registerPattern("application*.yml")
                .registerPattern("static/**");

        System.out.println("  RuntimeHints 创建成功!");
        System.out.println("  已注册反射提示: String, AotNativeDemo");
        System.out.println("  已注册资源提示: application*.yml, static/**");
    }

    /**
     * 演示 3: @RegisterReflectionForBinding 用法
     *
     * 这是 Spring 提供的简化注解，用于在 JSON 序列化/反序列化时
     * 自动注册反射提示（最常用的场景）。
     */
    private void demoReflectionRegistration() {
        System.out.println("""
                
                [3] @RegisterReflectionForBinding 简化注册
                --------------------------------------------
                
                === 方式 1: 在配置类上使用 ===
                \s""" + """
                @Configuration
                @RegisterReflectionForBinding({
                    UserDto.class,
                    OrderDto.class,
                    ProductDto.class
                })
                public class JsonConfig {
                    // 这些 DTO 类在 JSON 序列化时会自动注册反射提示
                    // 不需要手动调用 RuntimeHints.reflection().registerType()
                }
                """);

        System.out.println("""
                === 方式 2: 实现 RuntimeHintsRegistrar 接口 ===
                （更灵活，可以注册各种类型的提示）
                \s""" + """
                public class CustomRuntimeHints implements RuntimeHintsRegistrar {
                    @Override
                    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
                        // 反射提示
                        hints.reflection()
                            .registerType(UserDto.class, MemberCategory.values())
                            .registerType(OrderDto.class, MemberCategory.values());
                        
                        // 资源提示（打包配置文件到 Native Image）
                        hints.resources()
                            .registerPattern("mapper/*.xml")  // MyBatis mapper
                            .registerPattern("config/*.yml");
                        
                        // 代理提示（AOP 需要）
                        hints.proxies()
                            .registerJdkProxy(UserService.class);
                        
                        // 序列化提示
                        hints.serialization()
                            .registerType(UserDto.class)
                            .registerType(OrderDto.class);
                    }
                }
                
                // 在配置类上导入
                @Configuration
                @ImportRuntimeHints(CustomRuntimeHints.class)
                public class AppConfig { }
                """);

        System.out.println("""
                === 方式 3: 在 @Bean 方法上使用 @Reflective ===
                （Spring 6.2+ 新增，标注在需要反射访问的方法上）
                \s""" + """
                @Configuration
                public class MyConfig {
                    @Bean
                    @Reflective  // 告诉 AOT 保留此方法的反射信息
                    public MyService myService() {
                        return new MyService();
                    }
                }
                """);
    }

    /**
     * 演示 4: Native Image 编译命令和注意事项
     */
    private void demoNativeImageCommands() {
        System.out.println("""
                
                [4] Native Image 编译命令
                --------------------------------------------
                
                === 环境准备 ===
                # 1. 安装 GraalVM (推荐使用 SDKMAN)
                sdk install java 21.0.5-graal
                sdk use java 21.0.5-graal
                
                # 2. 验证安装
                native-image --version
                
                # 3. 安装 native-image 组件（如果缺少）
                gu install native-image
                
                === Maven 编译命令 ===
                # 方式 1: 直接编译为本地可执行文件
                mvn -Pnative native:compile
                
                # 方式 2: 构建 Docker 镜像（推荐，避免环境问题）
                mvn spring-boot:build-image -Pnative
                
                # 方式 3: 使用 Paketo Buildpacks
                mvn spring-boot:build-image
                
                === Gradle 编译命令 ===
                # 构建本地可执行文件
                ./gradlew nativeCompile
                
                # 构建 Docker 镜像
                ./gradlew bootBuildImage
                
                === 运行 Native Image ===
                # 直接运行
                ./target/spring-advanced-demo
                
                # 带参数运行
                ./target/spring-advanced-demo --server.port=8080
                
                === 注意事项 ===
                1. 首次编译需要下载大量依赖（~10分钟）
                2. 增量编译可以显著加快速度（~30秒）
                3. 需要在目标平台编译（Linux 二进制只能在 Linux 运行）
                4. 交叉编译可以使用 Docker（推荐方式）
                5. 确保所有反射/动态代理都配置了 RuntimeHints
                6. 测试 Native Image 是否有问题：
                   mvn -PnativeTest test
                """);
    }

    /**
     * 演示 5: JVM 启动 vs Native 启动时间对比
     */
    private void demoComparison() {
        System.out.println("""
                [5] JVM vs Native Image 性能对比
                --------------------------------------------
                
                === 启动时间对比 ===
                \s""" + """
                | 场景              | JVM (CDS)    | Native Image |
                |------------------|--------------|--------------|
                | 简单 Web 应用     | ~1.5s        | ~0.05s       |
                | Spring Boot 标准  | ~2.5s        | ~0.08s       |
                | 包含 JPA + 多 Bean| ~4.0s        | ~0.15s       |
                | Serverless 冷启动 | ~3.0s        | ~0.05s       |
                """);

        System.out.println("""
                === 内存占用对比 ===
                \s""" + """
                | 场景              | JVM          | Native Image |
                |------------------|--------------|--------------|
                | 启动后 RSS        | ~200MB       | ~50MB        |
                | 稳态 RSS         | ~300MB       | ~80MB        |
                | 空闲 5 分钟后     | ~150MB       | ~40MB        |
                """);

        System.out.println("""
                === 吞吐量对比 (ops/sec) ===
                \s""" + """
                | 场景              | JVM (JIT)    | Native Image |
                |------------------|--------------|--------------|
                | JSON 序列化       | ~50,000      | ~60,000      |
                | 数据库查询        | ~5,000       | ~4,800       |
                | 纯计算           | ~100,000     | ~120,000     |
                | HTTP 请求处理     | ~20,000      | ~18,000      |
                
                [结论]
                - 启动速度: Native 快 30-60 倍
                - 内存: Native 少 60-75%
                - 峰值性能: Native 略快或持平
                - 预热后性能: JVM 的 JIT 编译可能在长时间运行后反超
                - 适合场景: 
                  * 云原生/Serverless -> Native Image (快速弹性伸缩)
                  * 长时间运行的服务 -> JVM (JIT 优化更成熟)
                  * 边缘计算/IoT -> Native Image (资源受限)
                """);
    }

    /**
     * 演示 6: 面试考点总结
     */
    private void demoInterviewTips() {
        System.out.println("""
                [6] 面试考点总结
                --------------------------------------------
                
                Q: Spring AOT 的原理是什么？
                A: 在编译时（而非运行时）处理 Bean 定义：
                   1. 启动 ApplicationContext 收集所有 Bean 定义
                   2. 对每个 Bean 进行静态分析（去除反射，生成直接调用代码）
                   3. 处理 @Conditional 条件（确定最终 Bean 集合）
                   4. 生成 RuntimeHints 供 Native Image 使用
                   5. 输出 Java 源文件（不是字节码）到 aot-generated 目录
                
                Q: Native Image 为什么启动这么快？
                A: 1. 不需要 JVM 启动（直接是机器码）
                   2. 不需要类加载和验证（编译时已完成）
                   3. 不需要 JIT 编译（已经是优化后的代码）
                   4. 不需要 Spring 的 Bean 扫描和反射（AOT 已处理）
                   5. 使用了 Class Data Sharing (CDS) 类似的预初始化
                
                Q: Native Image 有什么限制？
                A: 1. 不支持运行时类加载（ClassLoader.defineClass）
                   2. 反射需要预先注册（RuntimeHints）
                   3. 动态代理需要预先声明
                   4. 某些库可能不兼容（需检查 GraalVM 兼容性列表）
                   5. 序列化框架需要特殊配置
                   6. 编译时间长（首次 ~10 分钟）
                
                Q: 如何让现有 Spring 项目支持 Native Image？
                A: 1. 升级 Spring Boot 到 3.x+（推荐 4.x）
                   2. 添加 spring-boot-starter-parent 的 native profile
                   3. 运行 AOT 测试：mvn -PnativeTest test
                   4. 修复不兼容问题（添加 RuntimeHints）
                   5. 逐步验证功能完整性
                   6. 常见不兼容库：CGLIB 动态代理、Groovy 脚本、部分 ORM 框架
                
                Q: AOT 处理和 CDS (Class Data Sharing) 的区别？
                A: - CDS：JVM 特性，共享已加载的类数据（减少启动时间 20-30%）
                   - AOT：编译时优化，去除运行时反射（减少启动时间 90%+）
                   - 两者互补：AOT + CDS 可以进一步加速 Native Image
                """);
    }
}

/**
 * 自定义 RuntimeHintsRegistrar 实现
 *
 * 【教学说明】
 * 这是一个完整的 RuntimeHintsRegistrar 实现示例，
 * 展示了如何为项目注册各种运行时提示。
 *
 * 【使用方式】
 * 在任何 @Configuration 类上添加：
 *   @ImportRuntimeHints(CustomRuntimeHints.class)
 *
 * 【面试考点】
 * RuntimeHintsRegistrar 是 Spring AOT 的核心扩展点，
 * 允许开发者告诉 Native Image 编译器哪些动态特性需要保留。
 *
 * @author ibqy
 * @since 2026-09-21
 */
class CustomRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // =============================================
        // 1. 反射提示 - 告诉 Native Image 保留反射访问
        // =============================================

        // 注册 DTO 类（JSON 序列化需要反射读取字段）
        hints.reflection()
                .registerType(
                        TypeReference.of(String.class),
                        MemberCategory.DECLARED_FIELDS,          // 允许访问字段
                        MemberCategory.INVOKE_DECLARED_METHODS   // 允许调用方法
                );

        // 也可以注册整个包下的所有类
        // hints.reflection().registerType(TypeReference.of("com.ibqy.spring.advanced.dto.*"));

        // =============================================
        // 2. 资源提示 - 告诉 Native Image 打包静态资源
        // =============================================
        hints.resources()
                // MyBatis mapper XML 文件
                .registerPattern("mapper/**/*.xml")
                // 配置文件
                .registerPattern("application*.yml")
                .registerPattern("application*.properties")
                // 静态资源
                .registerPattern("static/**")
                // 模板文件
                .registerPattern("templates/**");

        // =============================================
        // 3. 代理提示 - 告诉 Native Image 保留动态代理
        // =============================================
        // 如果使用 AOP，相关接口需要注册代理提示
        // hints.proxies().registerJdkProxy(MyServiceInterface.class);

        // =============================================
        // 4. 序列化提示 - 告诉 Native Image 保留序列化支持
        // =============================================
        // hints.serialization().registerType(MyDto.class);

        // =============================================
        // 5. JNI 提示 - 如果有 native 方法
        // =============================================
        // hints.jni().registerType(MyNativeClass.class);

        System.out.println("[CustomRuntimeHints] RuntimeHints 注册完成");
        System.out.println("  - 反射提示: String (fields + methods)");
        System.out.println("  - 资源提示: mapper/*.xml, application*.yml, static/**, templates/**");
        System.out.println("  - 代理提示: (按需添加)");
        System.out.println("  - 序列化提示: (按需添加)");
    }
}
