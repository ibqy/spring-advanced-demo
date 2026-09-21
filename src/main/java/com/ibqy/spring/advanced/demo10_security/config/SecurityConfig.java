package com.ibqy.spring.advanced.demo10_security.config;

import com.ibqy.spring.advanced.demo10_security.service.CustomUserDetailsService;
import com.ibqy.spring.advanced.demo10_security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 配置类 —— 使用 Lambda DSL（推荐方式）
 *
 * <h2>Spring Security 6 新特性</h2>
 * <ul>
 *     <li><b>Lambda DSL</b>：{@code http.authorizeHttpRequests(auth -> auth...)} 替代链式配置</li>
 *     <li><b>移除 MVC 相关配置</b>：Security 不再关心 MVC 细节</li>
 *     <li><b>@EnableMethodSecurity</b>：替代 @EnableGlobalMethodSecurity（已废弃）</li>
 *     <li><b>SecurityContext 默认不再写入 HttpSession</b>：无状态 API 更简单</li>
 *     <li><b>虚拟线程兼容</b>：SecurityContext 使用 ScopedValue 或 InheritableThreadLocal</li>
 * </ul>
 *
 * <h2>安全过滤器链</h2>
 * <pre>
 *   请求 → SecurityFilterChain
 *        → SecurityContextPersistenceFilter  (恢复/保存 SecurityContext)
 *        → UsernamePasswordAuthenticationFilter  (表单登录)
 *        → JwtAuthenticationFilter (我们自定义的！)
 *        → ExceptionTranslationFilter  (异常处理)
 *        → FilterSecurityInterceptor  (授权检查)
 *        → Controller
 * </pre>
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>Lambda DSL 比旧方式好在哪？→ 编译时类型检查、更好的代码可读性、避免错误配置</li>
 *     <li>@EnableWebSecurity 和 @EnableWebMvc 的关系？→ 它们独立工作。
 *     Security 配置过滤器链，MVC 配置请求映射</li>
 *     <li>SessionCreationPolicy.STATELESS 的作用？→ 告诉 Spring Security 不创建 HttpSession，
 *     适合 RESTful API</li>
 *     <li>SecurityContextHolder 的策略？→ MODE_THREADLOCAL（默认）、MODE_INHERITABLETHREADLOCAL
 *     （子线程继承）、MODE_GLOBAL（全局共享）</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see JwtUtil
 * @see CustomUserDetailsService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用 @PreAuthorize / @PostAuthorize 方法级别安全
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // ====================== 安全过滤器链配置 ======================

    /**
     * 核心安全配置 —— 使用 Lambda DSL
     *
     * <p>这是 Spring Security 6 推荐的方式，相比旧的 antMatchers 链式调用：
     * <ul>
     *     <li>有编译时类型检查（IDE 可以提示错误）</li>
     *     <li>代码更清晰，不会忘记配置某个环节</li>
     * </ul>
     *
     * <p>面试考点：authorizeHttpRequests 和 authorizeRequests 的区别？
     * → authorizeHttpRequests 是新版 API（基于 AuthorizationManager），
     * authorizeRequests 是旧版 API（基于 AccessDecisionManager，已废弃）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（JWT 方案不需要，因为不用 Cookie 认证）
                .csrf(csrf -> csrf.disable())

                // 设置无状态 Session（不使用 HttpSession）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 登录接口放行
                        .requestMatchers("/api/auth/login").permitAll()
                        // Actuator 端点放行（生产环境应该限制为 ADMIN）
                        .requestMatchers("/actuator/**").permitAll()
                        // 公开信息页面放行
                        .requestMatchers("/api/demo10/public/**").permitAll()
                        // 其它所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 配置 HTTP Basic 作为后备认证方式（方便测试 Actuator 端点）
                .httpBasic(Customizer.withDefaults())

                // 异常处理：未认证返回 401 而不是重定向到登录页
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("""
                                    {"error":"未认证","message":"请先登录获取 Token"}
                                    """);
                        })
                )

                // 添加 JWT 认证过滤器（在 UsernamePasswordAuthenticationFilter 之前执行）
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        // 面试考点：SecurityContext 与虚拟线程
        // Spring Security 6.4+ 支持使用 ScopedValue 传播 SecurityContext 到虚拟线程
        // 默认使用 SecurityContextHolder.MODE_THREADLOCAL
        // 如果启用了虚拟线程，Spring Boot 会自动配置为合适的策略
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        System.out.println("[SecurityConfig] SecurityContext 策略: INHERITABLETHREADLOCAL (兼容虚拟线程)");

        return http.build();
    }

    // ====================== JWT 认证过滤器 ======================

    /**
     * JWT 认证过滤器
     *
     * <p>继承 {@link OncePerRequestFilter}，确保每个请求只执行一次。
     * <p>工作流程：
     * <ol>
     *     <li>从 Authorization 头中提取 Bearer Token</li>
     *     <li>验证 Token 的签名和有效期</li>
     *     <li>如果有效，将用户信息设置到 SecurityContext 中</li>
     * </ol>
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                // 1. 提取 Authorization 头
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    // 2. 验证 Token
                    JwtUtil.JwtClaims claims = JwtUtil.validateToken(token);

                    if (claims != null) {
                        // 3. Token 有效 → 设置 SecurityContext
                        // 从 UserDetailsService 加载完整的用户信息（包括权限）
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(claims.subject());

                            // 创建认证对象
                            var authentication = new org.springframework.security.authentication
                                    .UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // 密码不需要了（已经通过 JWT 验证）
                                    userDetails.getAuthorities()
                            );

                            // 将认证信息存入 SecurityContext
                            // 后续可以通过 SecurityContextHolder.getContext().getAuthentication() 获取
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.debug("[JWT过滤器] 认证成功: {} (角色: {})",
                                    claims.subject(), claims.role());
                        } catch (Exception e) {
                            log.warn("[JWT过滤器] 用户加载失败: {}", e.getMessage());
                        }
                    }
                }

                // 继续过滤器链（无论 JWT 是否验证成功）
                // 如果 SecurityContext 中没有认证信息，后续的授权检查会拒绝请求
                filterChain.doFilter(request, response);
            }
        };
    }

    // ====================== 认证管理器 ======================

    /**
     * 认证管理器 —— 处理登录认证
     *
     * <p>{@link AuthenticationManager} 是 Spring Security 认证的核心接口。
     * <p>使用 {@link DaoAuthenticationProvider}（从数据库加载用户 + 密码比对）。
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    /**
     * 密码编码器
     *
     * <p>面试考点：为什么不用 MD5？→ MD5 太容易被暴力破解！
     * <ul>
     *     <li>BCrypt：经典选择，自带盐值，可配置强度</li>
     *     <li>PBKDF2：基于密码的密钥派生函数，FIPS 标准推荐</li>
     *     <li>Argon2：最新的密码哈希算法，抗 GPU 攻击</li>
     * </ul>
     *
     * <p>这里使用 {noop} 前缀配合 PasswordEncoder，
     * 因为我们的模拟数据使用明文密码（教学用）。
     * 实际项目中应该用 BCrypt 加密存储。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 支持多种密码编码格式：{noop}明文、{bcrypt}BCrypt、{pbkdf2}PBKDF2
        // DelegatingPasswordEncoder 根据密码前缀（如 {noop}、{bcrypt}）选择对应的编码器
        java.util.Map<String, PasswordEncoder> encoders = new java.util.HashMap<>();
        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
        encoders.put("bcrypt", new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        encoders.put("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        return new org.springframework.security.crypto.password.DelegatingPasswordEncoder("noop", encoders);
    }
}
