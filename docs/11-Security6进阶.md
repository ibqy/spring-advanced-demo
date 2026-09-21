# 11 · Spring Security 6 进阶

> Lambda DSL 配置、JWT 无状态认证、@PreAuthorize 方法安全 —— 构建零信任架构。

## 概述

Spring Security 6 带来了全面的 Lambda DSL 配置风格、更简洁的安全过滤链、以及对 JWT 无状态认证的深度支持。本 Demo 展示如何用最现代的 API 构建完整的安全体系。

## 核心概念

### Spring Security 6 架构

```
                    ┌──────────────────────────────────┐
                    │         Security Filter Chain     │
                    │                                    │
  HTTP Request ────▶│  ┌─────────────────────────────┐  │
                    │  │ SecurityContextPersistenceFilter│ │
                    │  └─────────────────────────────┘  │
                    │  ┌─────────────────────────────┐  │
                    │  │ UsernamePasswordAuthFilter    │ │
                    │  │ 或 JwtAuthenticationFilter   │ │
                    │  └─────────────────────────────┘  │
                    │  ┌─────────────────────────────┐  │
                    │  │ ExceptionTranslationFilter    │ │
                    │  └─────────────────────────────┘  │
                    │  ┌─────────────────────────────┐  │
                    │  │ FilterSecurityInterceptor     │ │
                    │  └─────────────────────────────┘  │
                    └──────────────────────────────────┘
                                    │
                                    ▼
                          ┌──────────────────┐
                          │  Controller      │
                          │  (@PreAuthorize) │
                          └──────────────────┘
```

### Lambda DSL vs 旧式配置

```java
// 旧式（Spring Security 5.x）
http
    .csrf().disable()
    .sessionManagement().sessionCreationPolicy(STATELESS)
    .and()
    .authorizeRequests()
        .antMatchers("/api/public/**").permitAll()
        .anyRequest().authenticated()
    .and()
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

// 新式 Lambda DSL（Spring Security 6.x）
http
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/public/**").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

## 代码走读

### 1. Security 配置（Lambda DSL）

```java
// demo10/security/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 启用 @PreAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint entryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 禁用（无状态 API 不需要）
            .csrf(csrf -> csrf.disable())

            // 无状态会话
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 请求授权规则
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/actuator/health",
                    "/h2-console/**"
                ).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // JWT 过滤器
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            // 异常处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((request, response, denied) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("""
                        {"error": "ACCESS_DENIED", "message": "权限不足"}
                    """);
                })
            )

            // H2 控制台支持（开发环境）
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 2. JWT 工具类

```java
// demo10/security/jwt/JwtUtils.java
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}")  // 24 hours
    private long expiration;

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
```

### 3. JWT 认证过滤器

```java
// demo10/security/jwt/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtUtils.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtils.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource()
                        .buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### 4. 方法安全

```java
// demo10/security/controller/SecureController.java
@RestController
@RequestMapping("/api")
public class SecureController {

    // 只有 ADMIN 角色可以访问
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 只有当前用户可以访问自己的数据
    @PreAuthorize("#username == authentication.name")
    @GetMapping("/users/{username}/profile")
    public UserProfile getProfile(@PathVariable String username) {
        return userService.getProfile(username);
    }

    // SpEL 表达式：检查自定义权限
    @PreAuthorize("hasAuthority('user:write')")
    @PostMapping("/users")
    public User createUser(@RequestBody @Valid UserRequest request) {
        return userService.create(request);
    }

    // 复杂条件：ADMIN 或资源所有者
    @PreAuthorize("hasRole('ADMIN') or #id == @userService.getOwnerId(#id)")
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    // 获取当前认证用户
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal UserDetails user) {
        return Map.of(
            "username", user.getUsername(),
            "authorities", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList()
        );
    }
}
```

### 5. 认证控制器

```java
// demo10/security/controller/AuthController.java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtils.generateToken(
                (UserDetails) authentication.getPrincipal());

        return new LoginResponse(token, "Bearer");
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody @Valid RegisterRequest request) {
        return userService.register(request);
    }
}

public record LoginRequest(String username, String password) {}
public record LoginResponse(String token, String type) {}
public record RegisterRequest(String username, String password, String email) {}
```

## 注解与 API 参考

| 注解 / API | 说明 |
|-----------|------|
| `@EnableWebSecurity` | 启用 Web 安全 |
| `@EnableMethodSecurity` | 启用方法级安全（替代 `@EnableGlobalMethodSecurity`） |
| `SecurityFilterChain` | 安全过滤链 Bean |
| `@PreAuthorize` | 方法调用前鉴权（SpEL 表达式） |
| `@PostAuthorize` | 方法调用后鉴权 |
| `@AuthenticationPrincipal` | 注入当前认证用户 |
| `JwtAuthenticationConverter` | JWT → Authentication 转换器 |
| `OncePerRequestFilter` | 每次请求只执行一次的过滤器 |
| `PasswordEncoder` | 密码编码器接口 |
| `SessionCreationPolicy.STATELESS` | 无状态会话策略 |

## 面试考点

::: warning 高频面试题
1. **Spring Security 6 的 Lambda DSL 有什么好处？**
   - 类型安全：编译期检查配置错误
   - 可读性更好：每个配置项的作用域清晰
   - 不再需要链式 `.and()` 调用
   - 更好的 IDE 支持

2. **JWT 认证流程是什么？**
   - 客户端 POST 用户名密码 → 验证成功 → 返回 JWT
   - 后续请求携带 `Authorization: Bearer <token>`
   - JwtFilter 解析 Token → 设置 SecurityContext
   - Controller 通过 `@AuthenticationPrincipal` 获取用户

3. **@PreAuthorize 中 SpEL 表达式怎么写？**
   - `hasRole('ADMIN')` — 角色检查
   - `hasAuthority('user:write')` — 权限检查
   - `#username == authentication.name` — 变量比较
   - `@beanName.method()` — 调用 Bean 方法
:::

## 常见陷阱

::: danger 陷阱 1：CSRF 保护与无状态 API
无状态 REST API（使用 JWT）应禁用 CSRF：`.csrf(csrf -> csrf.disable())`。但如果应用有 Cookie 认证的页面，则不应禁用。
:::

::: danger 陷阱 2：JWT 密钥泄露
`jwt.secret` 绝不能硬编码在代码中或提交到 Git。使用环境变量或配置中心：
```yaml
jwt:
  secret: ${JWT_SECRET:your-default-dev-key}
```
:::

::: danger 陷阱 3：@EnableGlobalMethodSecurity 已废弃
Spring Security 6 中，使用 `@EnableMethodSecurity` 替代 `@EnableGlobalMethodSecurity`。后者在 Security 6 中已被移除。
:::

::: danger 陷阱 4：SecurityContext 丢失
使用虚拟线程或 `@Async` 时，SecurityContext 可能不会自动传播。需要配置：
```java
@Bean
public SecurityContextHolderStrategy securityContextHolderStrategy() {
    return SecurityContextHolder.createInheritableThreadLocalContext();
}
```
:::

## 延伸阅读

- [Spring Security 6 迁移指南](https://docs.spring.io/spring-security/reference/migration/index.html)
- [JWT 认证指南](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Lambda DSL 配置](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
- [方法安全](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
