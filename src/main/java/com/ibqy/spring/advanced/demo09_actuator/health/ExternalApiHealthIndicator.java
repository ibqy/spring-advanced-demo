package com.ibqy.spring.advanced.demo09_actuator.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自定义健康检查指示器 —— 检查外部 API 依赖的健康状态
 *
 * <h3>Spring Boot 健康检查体系</h3>
 * <pre>
 *   GET /actuator/health
 *   {
 *     "status": "UP",                    ← 所有 HealthIndicator 的聚合状态
 *     "components": {
 *       "db": { "status": "UP", ... },  ← DataSourceHealthIndicator
 *       "diskSpace": { "status": "UP" }, ← DiskSpaceHealthIndicator
 *       "ping": { "status": "UP" },     ← PingHealthIndicator
 *       "externalApi": {               ← 我们自定义的！
 *         "status": "UP",
 *         "details": { "url": "...", "responseTime": "..." }
 *       }
 *     }
 *   }
 * </pre>
 *
 * <h3>Health 状态</h3>
 * <ul>
 *     <li>{@code UP}：组件正常运行</li>
 *     <li>{@code DOWN}：组件故障（会影响整体健康状态为 DOWN）</li>
 *     <li>{@code OUT_OF_SERVICE}：组件停止服务</li>
 *     <li>{@code UNKNOWN}：状态未知</li>
 * </ul>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>自定义 HealthIndicator 有什么用？→ 让运维团队在监控面板上看到所有依赖的健康状态</li>
 *     <li>健康检查频率？→ 默认每次请求 /actuator/health 时实时检查。
 *     可通过 health.endpoint.cache.time-to-live 设置缓存</li>
 *     <li>DOWN 会影响应用运行吗？→ 不会！健康状态只是监控信息，不会阻止请求处理。
 *     但 Kubernetes 的 liveness/readiness probe 会根据它决定是否重启 Pod</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {

    /**
     * 要检查的外部 API 地址
     * <p>面试考点：生产环境中这个值应该从配置中心读取，而不是硬编码。
     */
    private static final String EXTERNAL_API_URL = "https://httpbin.org/get";

    /**
     * 连接超时时间（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 3000;

    /**
     * 上次检查的结果缓存
     */
    private final AtomicReference<Health> lastHealth = new AtomicReference<>();

    /**
     * 执行健康检查
     *
     * <p>这个方法会在每次访问 /actuator/health 时被调用。
     * <p>使用 {@link Health.Builder} 构建检查结果，包含状态和详细信息。
     *
     * @return 健康检查结果
     */
    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();

        try {
            // 创建 HTTP 连接检查外部 API 可达性
            URI uri = URI.create(EXTERNAL_API_URL);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(CONNECT_TIMEOUT);

            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            connection.disconnect();

            if (responseCode >= 200 && responseCode < 400) {
                // 外部 API 正常
                Health health = Health.up()
                        .withDetail("url", EXTERNAL_API_URL)
                        .withDetail("responseCode", responseCode)
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("checkedAt", LocalDateTime.now().toString())
                        .build();
                lastHealth.set(health);
                return health;
            } else {
                // 外部 API 返回异常状态码
                Health health = Health.down()
                        .withDetail("url", EXTERNAL_API_URL)
                        .withDetail("responseCode", responseCode)
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("error", "Unexpected response code: " + responseCode)
                        .build();
                lastHealth.set(health);
                return health;
            }

        } catch (Exception e) {
            // 连接失败（网络超时、DNS 解析失败等）
            long responseTime = System.currentTimeMillis() - startTime;
            Health health = Health.down()
                    .withDetail("url", EXTERNAL_API_URL)
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("error", e.getClass().getSimpleName() + ": " + e.getMessage())
                    .withDetail("hint", "请检查网络连接和外部服务状态")
                    .build();
            lastHealth.set(health);
            return health;
        }
    }

    /**
     * 获取上次检查的结果（供自定义端点使用）
     */
    public Health getLastHealth() {
        return lastHealth.get();
    }
}
