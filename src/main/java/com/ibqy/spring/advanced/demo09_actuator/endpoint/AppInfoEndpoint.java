package com.ibqy.spring.advanced.demo09_actuator.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义 Actuator 端点 —— 暴露应用运行时信息
 *
 * <h3>Actuator 端点模型</h3>
 * <pre>
 *   HTTP 方法     →  @Endpoint 操作
 *   ─────────────────────────────────
 *   GET          →  @ReadOperation     （查询）
 *   POST         →  @WriteOperation    （修改）
 *   DELETE       →  @DeleteOperation   （删除）
 * </pre>
 *
 * <h3>端点注册</h3>
 * <p>标注了 {@code @Endpoint} 的类会被 Spring Boot Actuator 自动发现并注册为端点。
 * 端点的 base path 默认是 {@code /actuator}，可通过配置修改。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>自定义端点如何暴露到 Web？→ 需要 application.yml 中配置 management.endpoints.web.exposure.include</li>
 *     <li>@Endpoint vs @WebEndpoint vs @ControllerEndpoint？→
 *     @Endpoint 同时支持 Web 和 JMX；@WebEndpoint 只暴露 Web；
 *     Spring Boot 3+ 推荐只用 @Endpoint + @EndpointWebExtension</li>
 *     <li>端点参数如何绑定？→ @Selector 用于路径参数，@ReadOperation 的参数自动从查询参数绑定</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
@Endpoint(id = "appInfo")
public class AppInfoEndpoint {

    /**
     * 启动时间（用于计算运行时长）
     */
    private final Instant startTime = Instant.now();

    /**
     * 模拟的配置存储（实际项目中可能是数据库或配置中心）
     */
    private final Map<String, String> appConfig = new ConcurrentHashMap<>();

    /**
     * API 调用计数器
     */
    private final AtomicLong apiCallCount = new AtomicLong(0);

    public AppInfoEndpoint() {
        // 初始化一些默认配置
        appConfig.put("feature.newDashboard", "true");
        appConfig.put("feature.darkMode", "true");
        appConfig.put("maintenance.mode", "false");
    }

    // ====================== @ReadOperation（GET） ======================

    /**
     * 读取应用信息
     *
     * <p>对应请求：GET /actuator/appInfo
     *
     * @return 应用运行时信息的 Map
     */
    @ReadOperation
    public Map<String, Object> getAppInfo() {
        apiCallCount.incrementAndGet();

        Map<String, Object> info = new LinkedHashMap<>();

        // 基本信息
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("name", "Spring Advanced Demo");
        basic.put("version", "1.0.0");
        basic.put("author", "ibqy");
        basic.put("framework", "Spring Boot 4.1.1 + Spring Framework 7.0.8");
        basic.put("javaVersion", System.getProperty("java.version"));
        info.put("basic", basic);

        // 运行时间
        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("startTime", startTime.toString());
        runtime.put("uptime", calculateUptime());
        runtime.put("currentTime", LocalDateTime.now().toString());
        info.put("runtime", runtime);

        // JVM 信息
        Map<String, Object> jvm = new LinkedHashMap<>();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        jvm.put("vmName", runtimeMXBean.getVmName());
        jvm.put("vmVersion", runtimeMXBean.getVmVersion());
        jvm.put("heapMemoryUsed", memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024) + "MB");
        jvm.put("heapMemoryMax", memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024) + "MB");
        jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("jvm", jvm);

        // 自定义配置
        info.put("config", new LinkedHashMap<>(appConfig));

        // 调用统计
        info.put("apiCallCount", apiCallCount.get());

        return info;
    }

    /**
     * 读取特定配置项
     *
     * <p>对应请求：GET /actuator/appInfo/{key}
     *
     * @param key 配置键名（路径参数）
     * @return 配置值或"未找到"提示
     */
    @ReadOperation
    public Map<String, String> getConfigByKey(@Selector String key) {
        apiCallCount.incrementAndGet();

        Map<String, String> result = new LinkedHashMap<>();
        String value = appConfig.get(key);
        if (value != null) {
            result.put("key", key);
            result.put("value", value);
        } else {
            result.put("key", key);
            result.put("value", null);
            result.put("message", "配置项不存在");
        }
        return result;
    }

    // ====================== @WriteOperation（POST） ======================

    /**
     * 更新配置项
     *
     * <p>对应请求：POST /actuator/appInfo
     * <p>请求体：{"key": "feature.newDashboard", "value": "false"}
     *
     * @param key   配置键名
     * @param value 配置值
     * @return 更新结果
     */
    @WriteOperation
    public Map<String, String> updateConfig(String key, String value) {
        apiCallCount.incrementAndGet();

        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配置键不能为空");
        }

        String oldValue = appConfig.put(key, value);
        System.out.printf("[AppInfo端点] 配置更新: %s = %s (旧值: %s)%n", key, value, oldValue);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", "updated");
        result.put("key", key);
        result.put("newValue", value);
        result.put("oldValue", oldValue);
        return result;
    }

    // ====================== @DeleteOperation（DELETE） ======================

    /**
     * 删除配置项
     *
     * <p>对应请求：DELETE /actuator/appInfo/{key}
     *
     * @param key 要删除的配置键名
     * @return 删除结果
     */
    @DeleteOperation
    public Map<String, String> deleteConfig(@Selector String key) {
        apiCallCount.incrementAndGet();

        String removedValue = appConfig.remove(key);

        Map<String, String> result = new LinkedHashMap<>();
        if (removedValue != null) {
            result.put("status", "deleted");
            result.put("key", key);
            result.put("removedValue", removedValue);
        } else {
            result.put("status", "not_found");
            result.put("key", key);
            result.put("message", "配置项不存在");
        }
        return result;
    }

    // ====================== 工具方法 ======================

    /**
     * 计算运行时长（人类可读格式）
     */
    private String calculateUptime() {
        long seconds = (Instant.now().getEpochSecond() - startTime.getEpochSecond());
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return "%d天 %d小时 %d分钟 %d秒".formatted(days, hours, minutes, secs);
    }

    /**
     * 供 WebExtension 调用的内部方法
     */
    public Map<String, String> getInternalConfig() {
        return new LinkedHashMap<>(appConfig);
    }
}
