# 05 · AOT & Native Image

> 编译时优化与 GraalVM 原生镜像 —— 毫秒级启动、极低内存。

## 概述

Spring Boot 3.x 引入了 **AOT（Ahead-of-Time）处理**，Spring Framework 7.0 / Boot 4.1 进一步优化了这一机制。AOT 在编译时完成 Bean 定义解析、代理生成等工作，使运行时启动更快。配合 GraalVM 可生成 **原生镜像（Native Image）**，实现毫秒级启动。

## 核心概念

### JVM 模式 vs AOT 模式 vs Native Image

```
┌─────────────────────────────────────────────────────────────────┐
│                     JVM 模式（传统）                              │
│  源码 → .class → JVM 启动 → 反射 → CGLIB 代理 → 运行时          │
│  启动时间: 2-5s | 内存: 200-500MB                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     AOT 模式（JVM 优化）                          │
│  源码 → AOT 处理 → 生成优化代码 → JVM 启动 → 运行时（更快）       │
│  启动时间: 1-3s | 内存: 150-300MB                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                     Native Image（GraalVM）                      │
│  源码 → AOT 处理 → GraalVM 编译 → 原生二进制 → 直接运行          │
│  启动时间: 50-200ms | 内存: 50-100MB                            │
└─────────────────────────────────────────────────────────────────┘
```

### AOT 处理做了什么？

| 阶段 | 传统 JVM | AOT 处理 |
|------|---------|---------|
| Bean 定义 | 运行时扫描 | 编译时生成代码 |
| CGLIB 代理 | 运行时动态生成 | 编译时静态生成 |
| 配置类解析 | 反射解析 | 直接调用代码 |
| 属性绑定 | 运行时解析 | 编译时生成绑定代码 |
| Spring EL | 运行时解析 | 编译时预计算 |

## 代码走读

### 1. 启用 AOT 处理

```bash
# AOT 处理（生成优化代码）
mvn spring-boot:process-aot

# 生成的代码位于
# target/classes/META-INF/spring-aot/
```

### 2. 反射注册（Native Image 必需）

GraalVM Native Image 在编译时确定所有类，运行时的反射需要显式注册：

```java
// demo04/aot/config/RuntimeHintsConfig.java
@Configuration
public class RuntimeHintsConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 注册反射访问的类
        hints.reflection()
                .registerType(MyEntity.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
                .registerType(MyDto.class, MemberCategory.DECLARED_FIELDS);

        // 注册资源文件
        hints.resources()
                .registerPattern("META-INF/services/*")
                .registerPattern("static/**");

        // 注册 JDK 代理
        hints.proxies()
                .registerJdkProxy(MyService.class);
    }
}

// 在配置类上声明
@Configuration
@ImportRuntimeHints(RuntimeHintsConfig.class)
public class AppConfig {
}
```

### 3. 使用 @RegisterReflectionForBinding 快捷注解

```java
// 快捷方式：注册需要序列化/反序列化的类
@RestController
@RegisterReflectionForBinding({User.class, Order.class, Product.class})
public class ApiController {

    @GetMapping("/api/users")
    public List<User> getUsers() {
        return userService.findAll();
    }
}
```

### 4. 构建 Native Image

```xml
<!-- pom.xml 添加 native profile -->
<profiles>
    <profile>
        <id>native</id>
        <properties>
            <spring-boot.build-image.enabled>true</spring-boot.build-image.enabled>
        </properties>
    </profile>
</profiles>
```

```bash
# 构建原生镜像（需要 GraalVM）
mvn -Pnative native:compile

# 运行
./target/spring-advanced-demo
```

## 注解与 API 参考

| 注解 / API | 说明 |
|-----------|------|
| `@RegisterReflectionForBinding` | 注册需要反射访问的类（序列化等） |
| `RuntimeHintsRegistrar` | 运行时提示注册器接口 |
| `@ImportRuntimeHints` | 导入 RuntimeHints 配置 |
| `RuntimeHints.reflection()` | 注册反射访问 |
| `RuntimeHints.resources()` | 注册资源文件 |
| `RuntimeHints.proxies()` | 注册 JDK 动态代理 |
| `MemberCategory` | 反射成员类别枚举 |

## 面试考点

::: warning 高频面试题
1. **AOT 和 JIT 有什么区别？**
   - AOT（Ahead-of-Time）：编译前/运行前处理，生成优化代码或原生二进制
   - JIT（Just-in-Time）：运行时编译，根据热点代码优化
   - AOT 牺牲开发便捷性换取启动速度和内存优化
   - JIT 启动慢但长期运行峰值性能更好

2. **Native Image 有什么限制？**
   - 不支持运行时动态类加载（反射需要显式注册）
   - 不支持某些 Java 特性（如 `sun.misc.Unsafe`）
   - 序列化/反序列化需要 `@RegisterReflectionForBinding`
   - CGLIB 代理需要在 AOT 阶段确定

3. **什么时候用 Native Image？什么时候用 JVM？**
   - Native Image：Serverless、容器冷启动、边缘计算
   - JVM：长期运行的服务、峰值性能要求高、大量使用反射/动态代理
:::

## 常见陷阱

::: danger 陷阱 1：忘记注册反射类
Native Image 中最常见的错误。如果运行时报 `ClassNotFoundException` 或反射异常，检查是否注册了所有需要反射访问的类。
:::

::: danger 陷阱 2：资源文件未打包
Native Image 默认不打包资源文件。需要在 `RuntimeHints` 中显式注册：
```java
hints.resources().registerPattern("templates/**");
```
:::

::: danger 陷阱 3：第三方库兼容性问题
并非所有第三方库都支持 GraalVM Native Image。使用 [GraalVM Reachability Metadata Repository](https://github.com/oracle/graalvm-reachability-metadata) 检查兼容性。
:::

## 延伸阅读

- [Spring Boot AOT 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/native-image.html)
- [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Spring AOT Optimization Guide](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#core.aot)
