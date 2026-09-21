# 04 · Virtual Threads

> Java 21 虚拟线程 —— 用同步代码写出异步性能。

## 概述

Java 21 正式引入了 **虚拟线程（Virtual Threads）**，这是 JDK 的重大改进。虚拟线程是轻量级线程，不与操作系统线程一一绑定，可以轻松创建百万级别的并发任务。本 Demo 展示如何在 Spring Boot 4.1 中集成虚拟线程。

## 核心概念

### 平台线程 vs 虚拟线程

```
┌─────────────────────────────────────────────────────────┐
│                    操作系统线程 (OS Thread)                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ Platform  │  │ Platform  │  │ Platform  │  │ Platform │ │
│  │ Thread 1  │  │ Thread 2  │  │ Thread 3  │  │ Thread N │ │
│  │ (1MB栈)   │  │ (1MB栈)   │  │ (1MB栈)   │  │ (1MB栈)  │ │
│  └─────┬────┘  └─────┬────┘  └─────┬────┘  └────┬────┘ │
│        │              │              │             │       │
└────────┼──────────────┼──────────────┼─────────────┼──────┘
         │              │              │             │
    ┌────▼────┐    ┌────▼────┐   ┌────▼────┐   ┌───▼─────┐
    │ Virtual │    │ Virtual │   │ Virtual │   │ Virtual │
    │ Thread  │    │ Thread  │   │ Thread  │   │ Thread  │
    │ (~1KB)  │    │ (~1KB)  │   │ (~1KB)  │   │ (~1KB)  │
    └─────────┘    └─────────┘   └─────────┘   └─────────┘
```

| 特性 | 平台线程 | 虚拟线程 |
|------|---------|---------|
| 内存占用 | ~1MB / 线程 | ~1KB / 线程 |
| 创建成本 | 高（涉及 OS 调用） | 极低 |
| 最大数量 | 数百到数千 | 百万级 |
| 调度方式 | OS 调度 | JVM 调度（ForkJoinPool） |
| 适用场景 | CPU 密集型 | I/O 密集型 |
| 线程池 | 必须使用 | 不需要池化（一次性） |

### 关键 API

```java
// 创建虚拟线程
Thread.ofVirtual().start(() -> {
    System.out.println("Hello from virtual thread!");
});

// 使用 ExecutorService
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> callRemoteApi());
    executor.submit(() -> queryDatabase());
}

// Spring Boot 一键启用
// application.yml:
// spring.threads.virtual.enabled: true
```

## 代码走读

### 1. 启用虚拟线程

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true    # 全局启用虚拟线程
```

这一行配置会让 Spring Boot 的 Tomcat、`@Async`、`@Scheduled` 等全部使用虚拟线程。

### 2. 对比演示 Controller

```java
// demo03/virtualthreads/controller/ConcurrencyController.java
@RestController
@RequestMapping("/demo03")
public class ConcurrencyController {

    private final ConcurrencyService service;

    @GetMapping("/platform-threads")
    public Map<String, Object> platformThreads(@RequestParam(defaultValue = "100") int count) {
        return service.runWithPlatformThreads(count);
    }

    @GetMapping("/virtual-threads")
    public Map<String, Object> virtualThreads(@RequestParam(defaultValue = "100") int count) {
        return service.runWithVirtualThreads(count);
    }

    @GetMapping("/info")
    public Map<String, Object> threadInfo() {
        return Map.of(
            "currentThread", Thread.currentThread().toString(),
            "isVirtual", Thread.currentThread().isVirtual(),
            "threadClass", Thread.currentThread().getClass().getName()
        );
    }
}
```

### 3. 服务层对比

```java
// demo03/virtualthreads/service/ConcurrencyService.java
@Service
public class ConcurrencyService {

    public Map<String, Object> runWithPlatformThreads(int count) {
        long start = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(10); // 传统线程池

        List<Future<String>> futures = IntStream.range(0, count)
                .mapToObj(i -> executor.submit(this::simulateIoTask))
                .toList();

        List<String> results = futures.stream()
                .map(f -> { try { return f.get(); } catch (Exception e) { return "error"; } })
                .toList();

        executor.close();
        long duration = System.currentTimeMillis() - start;

        return Map.of(
            "type", "platform",
            "count", count,
            "duration_ms", duration,
            "threadUsed", 10
        );
    }

    public Map<String, Object> runWithVirtualThreads(int count) {
        long start = System.currentTimeMillis();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.range(0, count)
                    .mapToObj(i -> executor.submit(this::simulateIoTask))
                    .toList();

            List<String> results = futures.stream()
                    .map(f -> { try { return f.get(); } catch (Exception e) { return "error"; } })
                    .toList();

            long duration = System.currentTimeMillis() - start;
            return Map.of(
                "type", "virtual",
                "count", count,
                "duration_ms", duration,
                "threadUsed", count  // 每个任务一个虚拟线程
            );
        }
    }

    private String simulateIoTask() {
        try {
            Thread.sleep(100); // 模拟 I/O 等待
            return "done-" + Thread.currentThread().threadId();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
    }
}
```

## 注解与 API 参考

| API | 说明 |
|-----|------|
| `Thread.ofVirtual()` | 创建虚拟线程构建器 |
| `Executors.newVirtualThreadPerTaskExecutor()` | 每个任务一个虚拟线程的线程池 |
| `Thread.isVirtual()` | 判断当前线程是否为虚拟线程 |
| `Thread.currentThread()` | 获取当前线程 |
| `Thread.threadId()` | 获取线程 ID |
| `spring.threads.virtual.enabled` | Spring Boot 全局开关 |

## 面试考点

::: warning 高频面试题
1. **虚拟线程和协程有什么区别？**
   - 虚拟线程是 JVM 层面的实现，对开发者透明（写法与普通线程一致）
   - 协程是语言层面的（如 Kotlin coroutines），需要 `suspend` 关键字
   - 虚拟线程不需要特殊的语法，迁移成本更低

2. **虚拟线程适用于什么场景？**
   - 最佳：I/O 密集型（网络调用、数据库查询、文件读写）
   - 不适合：CPU 密集型（纯计算，虚拟线程不会比平台线程快）
   - 不适合：需要 `synchronized` 大量阻塞的场景（会 pin 住载体线程）

3. **虚拟线程需要池化吗？**
   - 不需要！虚拟线程设计为"一次性使用"，用完即弃
   - 使用 `try-with-resources` 管理 `ExecutorService` 生命周期
   - `Executors.newVirtualThreadPerTaskExecutor()` 每次提交都创建新虚拟线程
:::

## 常见陷阱

::: danger 陷阱 1：synchronized 导致线程钉住（Pinning）
虚拟线程在 `synchronized` 块中阻塞时，会"钉住"载体线程（carrier thread），无法让出。应改用 `ReentrantLock`：
```java
// 避免
synchronized (lock) {
    Thread.sleep(1000);  // 钉住载体线程！
}

// 推荐
private final ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    Thread.sleep(1000);  // 可以正常 let 出
} finally {
    lock.unlock();
}
```
:::

::: danger 陷阱 2：ThreadLocal 内存泄漏
虚拟线程数量巨大，`ThreadLocal` 会创建大量副本。使用 ScopedValue（Java 21 预览特性）替代：
```java
// 避免
private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

// 推荐（Java 21+ Preview）
private static final ScopedValue<User> currentUser = ScopedValue.newInstance();
ScopedValue.runWhere(currentUser, user, () -> {
    // currentUser.get() 在此范围内可用
});
```
:::

::: danger 陷阱 3：误用固定线程池
```java
// 错误：虚拟线程 + 固定线程池 = 失去虚拟线程的优势
Executors.newFixedThreadPool(10); // 还是平台线程

// 正确
Executors.newVirtualThreadPerTaskExecutor();
```
:::

## 延伸阅读

- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot Virtual Threads 文档](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads)
- [Virtual Threads in Action - Spring I/O 2024](https://www.youtube.com/results?search_query=virtual+threads+spring)
