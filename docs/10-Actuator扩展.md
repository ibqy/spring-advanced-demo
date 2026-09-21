# 10 · Actuator 扩展

> 自定义 @Endpoint + HealthIndicator —— 打造运维友好的监控系统。

## 概述

Spring Boot Actuator 提供了生产级的监控和管理能力。除了内置的 `/health`、`/info`、`/metrics` 等端点外，Spring Boot 4.1 允许我们通过 `@Endpoint` 注解自定义端点，通过 `HealthIndicator` 实现业务级健康检查。

## 核心概念

### Actuator 端点架构

```
┌────────────────────────────────────────────────────┐
│                  Spring Boot Actuator               │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │            Built-in Endpoints                  │  │
│  │  /health  /info  /metrics  /env  /beans       │  │
│  │  /configprops  /mappings  /threaddump  ...    │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │            Custom Endpoints (本 Demo)           │  │
│  │                                                │  │
│  │  @Endpoint  ─── 自定义端点                     │  │
│  │  HealthIndicator ─── 自定义健康检查             │  │
│  │  @ReadOperation / @WriteOperation             │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  ┌───────────────────────────────────────────────┐  │
│  │            Exposure Layer                      │  │
│  │  Web (HTTP) ─── /actuator/...                  │  │
│  │  JMX ─── MBean                                 │  │
│  └───────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────┘
```

## 代码走读

### 1. 自定义 Endpoint

```java
// demo09/actuator/endpoint/CacheManagementEndpoint.java
@Endpoint(id = "cache")
public class CacheManagementEndpoint {

    private final CacheManager cacheManager;

    public CacheManagementEndpoint(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    // GET /actuator/cache
    @ReadOperation
    public Map<String, Object> getCacheInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                info.put(name, Map.of(
                    "size", estimateCacheSize(cache),
                    "nativeCacheType", cache.getNativeCache().getClass().getSimpleName()
                ));
            }
        });

        return info;
    }

    // POST /actuator/cache (清除缓存)
    @WriteOperation
    public String clearCache(@Selector String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            return "Cache '" + cacheName + "' cleared successfully";
        }
        return "Cache '" + cacheName + "' not found";
    }

    private long estimateCacheSize(Cache cache) {
        // 简化实现，实际可通过 NativeCache 获取
        return 0;
    }
}
```

### 2. 自定义 HealthIndicator

```java
// demo09/actuator/health/ExternalApiHealthIndicator.java
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    private final WebClient webClient;

    @Override
    public Health health() {
        try {
            // 检查外部 API 可用性
            ResponseEntity<String> response = webClient.get()
                    .uri("https://api.external-service.com/health")
                    .retrieve()
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("service", "External API")
                        .withDetail("responseTime", "< 3s")
                        .build();
            }

            return Health.down()
                    .withDetail("service", "External API")
                    .withDetail("status", response.getStatusCode())
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "External API")
                    .withException(e)
                    .build();
        }
    }
}
```

### 3. 数据库连接池健康检查

```java
// demo09/actuator/health/DatabasePoolHealthIndicator.java
@Component
public class DatabasePoolHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            // 验证连接可用
            if (conn.isValid(2)) {
                return Health.up()
                        .withDetail("database", "H2")
                        .withDetail("connection", "active")
                        .build();
            }
            return Health.down()
                    .withDetail("database", "H2")
                    .withDetail("error", "Connection not valid")
                    .build();
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("database", "H2")
                    .withException(e)
                    .build();
        }
    }
}
```

### 4. 聚合健康状态

```java
// 自定义 HealthAggregator（当多个 HealthIndicator 时，如何决定整体状态）
@Component
public class CustomHealthAggregator implements HealthAggregator {

    @Override
    public Health aggregate(Map<String, Health> healths) {
        Status aggregateStatus = Status.UP;

        for (Health health : healths.values()) {
            if (health.getStatus().equals(Status.DOWN)) {
                aggregateStatus = Status.DOWN;
                break;
            }
            if (health.getStatus().equals(Status.UNKNOWN)) {
                aggregateStatus = Status.UNKNOWN;
            }
        }

        return new Health.Builder(aggregateStatus)
                .withDetails(healths.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }
}
```

### 5. 注册端点为 Spring Bean

```java
// 确保自定义端点被 Spring 扫描
@Configuration
public class ActuatorConfig {

    @Bean
    public CacheManagementEndpoint cacheManagementEndpoint(CacheManager cacheManager) {
        return new CacheManagementEndpoint(cacheManager);
    }
}
```

### 6. 配置暴露规则

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,cache   # 包含自定义端点
  endpoint:
    health:
      show-details: always                   # 显示详细健康信息
      show-components: always
```

## 注解与 API 参考

| 注解 / API | 说明 |
|-----------|------|
| `@Endpoint(id = "xxx")` | 定义自定义端点 |
| `@WebEndpoint` | 仅 Web 暴露的端点 |
| `@JmxEndpoint` | 仅 JMX 暴露的端点 |
| `@ReadOperation` | GET 操作 |
| `@WriteOperation` | POST 操作 |
| `@DeleteOperation` | DELETE 操作 |
| `@Selector` | 路径变量参数注解 |
| `HealthIndicator` | 健康检查接口 |
| `Health.up() / Health.down()` | 构建健康状态 |
| `Health.Builder` | 健康状态构建器 |
| `Status` | 状态枚举（UP/DOWN/OUT_OF_SERVICE/UNKNOWN） |

## 面试考点

::: warning 高频面试题
1. **Actuator 端点的安全考虑？**
   - 生产环境绝不能暴露所有端点（`include: "*"`）
   - `/env` 可能泄露敏感配置
   - `/heapdump` 包含内存中的敏感数据
   - 推荐：只暴露 `health`、`prometheus`、`info`

2. **自定义 HealthIndicator 和 @Endpoint 的区别？**
   - HealthIndicator：影响 `/health` 端点的状态（UP/DOWN）
   - @Endpoint：创建独立的端点 URL，用于自定义操作
   - 健康检查是"被动监控"，自定义端点是"主动操作"

3. **如何让自定义端点同时支持 Web 和 JMX？**
   - 使用 `@Endpoint`（同时支持两种暴露方式）
   - `@WebEndpoint` 仅 Web
   - `@JmxEndpoint` 仅 JMX
:::

## 常见陷阱

::: danger 陷阱 1：HealthIndicator 阻塞应用启动
HealthIndicator 在应用启动时被调用。如果检查的外部服务不可达，可能导致应用标记为 DOWN。使用超时设置和降级策略。
:::

::: danger 陷阱 2：@WriteOperation 缺少权限控制
自定义的写操作端点没有默认的安全控制。必须通过 Spring Security 限制访问。
:::

::: danger 陷阱 3：端点 ID 冲突
自定义端点的 `id` 不能与内置端点重复，否则启动报错。
:::

## 延伸阅读

- [Spring Boot Actuator 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [自定义端点](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.custom)
- [HealthIndicator 接口](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/actuate/health/HealthIndicator.html)
