package com.ibqy.spring.advanced.demo07_data_jpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 配置类 —— 启用审计功能
 *
 * <h3>@EnableJpaAuditing</h3>
 * <p>启用 Spring Data JPA 的审计机制。启用后，标注了以下注解的字段会被自动填充：
 * <ul>
 *     <li>{@code @CreatedDate}：实体首次保存时填充当前时间</li>
 *     <li>{@code @LastModifiedDate}：实体每次更新时填充当前时间</li>
 *     <li>{@code @CreatedBy}：实体首次保存时填充当前用户（需配置 AuditorAware）</li>
 *     <li>{@code @LastModifiedBy}：实体每次更新时填充当前用户（需配置 AuditorAware）</li>
 * </ul>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>审计的时间源是什么？→ 默认使用系统时钟。可以通过注册 DateTimeProvider Bean 来替换</li>
 *     <li>AuditorAware 怎么获取当前用户？→ 可以从 SecurityContextHolder 获取，也可以从 ThreadLocal 获取</li>
 *     <li>审计字段会被覆盖吗？→ @CreatedDate 使用 updatable=false，不会被更新覆盖</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * 自定义 DateTimeProvider 示例（可选）
     *
     * <p>默认情况下，Spring Data JPA 使用 LocalDateTime.now() 作为审计时间源。
     * 如果需要自定义（如使用固定时区），可以注册一个 DateTimeProvider Bean：
     *
     * <pre>{@code
     * @Bean
     * public DateTimeProvider auditDateTimeProvider() {
     *     return () -> Optional.of(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
     * }
     * }</pre>
     *
     * <p>自定义 AuditorAware 示例（配合 Spring Security 使用）：
     *
     * <pre>{@code
     * @Bean
     * public AuditorAware<String> auditorAware() {
     *     return () -> Optional.ofNullable(SecurityContextHolder.getContext())
     *         .map(SecurityContext::getAuthentication)
     *         .filter(Authentication::isAuthenticated)
     *         .map(Authentication::getName);
     * }
     * }</pre>
     */
}
