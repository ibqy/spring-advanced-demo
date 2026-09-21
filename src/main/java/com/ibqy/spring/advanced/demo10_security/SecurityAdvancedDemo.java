package com.ibqy.spring.advanced.demo10_security;

import com.ibqy.spring.advanced.demo10_security.util.JwtUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Demo 10：Spring Security 6 进阶
 *
 * <h2>本 Demo 涵盖的核心知识点</h2>
 * <ol>
 *     <li><b>SecurityFilterChain Lambda DSL</b>：Spring Security 6 推荐的配置方式</li>
 *     <li><b>JWT 生成与验证</b>：纯 Java 实现 HMAC-SHA256，不依赖第三方 JWT 库</li>
 *     <li><b>@PreAuthorize / @PostAuthorize</b>：方法级别的安全控制</li>
 *     <li><b>SecurityContext 与虚拟线程</b>：兼容 Java 21 虚拟线程的上下文传播</li>
 *     <li><b>自定义 UserDetailsService</b>：从自定义数据源加载用户信息</li>
 * </ol>
 *
 * <h2>认证流程全景</h2>
 * <pre>
 *   客户端                           服务端
 *   ────                           ────
 *
 *   ① POST /auth/login ──────────→  AuthenticationManager.authenticate()
 *      {"username":"admin",           ↓
 *       "password":"admin123"}     UserDetailsService.loadUserByUsername()
 *                                   ↓
 *                                 PasswordEncoder.matches() → 验证密码
 *                                   ↓
 *   ←── {"token":"eyJ..."} ─────  JwtUtil.generateToken() → 返回 JWT
 *
 *   ② GET /admin/dashboard ─────→  JwtAuthenticationFilter
 *      Authorization: Bearer eyJ...   ↓
 *                                   JwtUtil.validateToken() → 验证签名和有效期
 *                                   ↓
 *                                   SecurityContextHolder.setAuthentication()
 *                                   ↓
 *                                   @PreAuthorize("hasRole('ADMIN')") → 检查权限
 *                                   ↓
 *   ←── {"data":...} ───────────  Controller 方法执行
 * </pre>
 *
 * <h2>测试指南</h2>
 * <pre>
 *   # 1. 登录获取 Token
 *   curl -X POST http://localhost:8080/api/demo10/auth/login \
 *     -H "Content-Type: application/json" \
 *     -d '{"username":"admin","password":"admin123"}'
 *
 *   # 2. 使用 Token 访问受保护接口
 *   curl http://localhost:8080/api/demo10/admin/dashboard \
 *     -H "Authorization: Bearer &lt;token&gt;"
 *
 *   # 3. 查看当前用户
 *   curl http://localhost:8080/api/demo10/user/me \
 *     -H "Authorization: Bearer &lt;token&gt;"
 *
 *   # 4. 测试数据级权限（普通用户尝试查看他人资料）
 *   # 先用 user 登录，然后尝试：
 *   curl http://localhost:8080/api/demo10/user/admin/profile \
 *     -H "Authorization: Bearer &lt;user_token&gt;"
 * </pre>
 *
 * <h2>面试高频问题</h2>
 * <ul>
 *     <li>JWT vs Session？→ JWT 无状态适合分布式，Session 有状态适合单体应用。
 *     JWT 的问题是没法主动吊销，需要配合黑名单</li>
 *     <li>Spring Security 的过滤器链执行顺序？→ 按 Bean 注册顺序，
 *     可以用 addFilterBefore/After 调整自定义过滤器的位置</li>
 *     <li>@EnableMethodSecurity 和 @EnableGlobalMethodSecurity？→ 后者已废弃，
 *     前者支持 @PreAuthorize/@PostAuthorize/@PreFilter/@PostFilter</li>
 *     <li>虚拟线程下 SecurityContext 怎么办？→ 用 MODE_INHERITABLETHREADLOCAL，
 *     或 Spring Security 6.4+ 的 ScopedValue 支持</li>
 * </ul>
 *
 * <h2>测试账号</h2>
 * <table>
 *     <tr><th>用户名</th><th>密码</th><th>角色</th></tr>
 *     <tr><td>admin</td><td>admin123</td><td>ADMIN, USER</td></tr>
 *     <tr><td>user</td><td>user123</td><td>USER</td></tr>
 *     <tr><td>editor</td><td>editor123</td><td>EDITOR, USER</td></tr>
 * </table>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see com.ibqy.spring.advanced.demo10_security.config.SecurityConfig
 * @see com.ibqy.spring.advanced.demo10_security.controller.SecureController
 * @see com.ibqy.spring.advanced.demo10_security.util.JwtUtil
 * @see com.ibqy.spring.advanced.demo10_security.service.CustomUserDetailsService
 */
@SpringBootApplication(scanBasePackages = "com.ibqy.spring.advanced.demo10_security")
public class SecurityAdvancedDemo {

    public static void main(String[] args) {
        SpringApplication.run(SecurityAdvancedDemo.class, args);
    }

    /**
     * 启动后执行 JWT 演示和打印测试指南
     */
    @Bean
    CommandLineRunner demoRunner() {
        return args -> {
            System.out.println("""
                    
                    ╔══════════════════════════════════════════════════════════╗
                    ║     Demo 10: Spring Security 6 进阶                     ║
                    ║     Lambda DSL · JWT · @PreAuthorize · 虚拟线程        ║
                    ╚══════════════════════════════════════════════════════════╝
                    """);

            // ==================== JWT 生成与验证演示 ====================
            System.out.println("========== JWT 生成与验证演示 ==========");
            System.out.printf("  密钥信息: %s%n", JwtUtil.getSecretKeyInfo());
            System.out.println();

            // 为 admin 用户生成 Token
            String adminToken = JwtUtil.generateToken("admin", "ADMIN");
            System.out.printf("  admin Token: %s%n", adminToken);
            System.out.printf("  Token 长度: %d 字符%n", adminToken.length());
            System.out.println("  Token 结构:");
            String[] parts = adminToken.split("\\.");
            System.out.printf("    Header:    %s%n", new String(java.util.Base64.getUrlDecoder().decode(parts[0])));
            System.out.printf("    Payload:   %s%n", new String(java.util.Base64.getUrlDecoder().decode(parts[1])));
            System.out.printf("    Signature: %s...%s (共%d字符)%n",
                    parts[2].substring(0, 10),
                    parts[2].substring(parts[2].length() - 6),
                    parts[2].length());
            System.out.println();

            // 验证 Token
            System.out.println("  验证 Token:");
            JwtUtil.JwtClaims claims = JwtUtil.validateToken(adminToken);
            if (claims != null) {
                System.out.printf("    ✓ 签名有效%n");
                System.out.printf("    ✓ 用户: %s%n", claims.subject());
                System.out.printf("    ✓ 角色: %s%n", claims.role());
                System.out.printf("    ✓ 过期时间: %s%n", claims.expirationFormatted());
            }

            // 验证被篡改的 Token
            System.out.println("\n  验证被篡改的 Token:");
            String tamperedToken = adminToken.substring(0, adminToken.length() - 3) + "abc";
            JwtUtil.JwtClaims tamperedClaims = JwtUtil.validateToken(tamperedToken);
            System.out.printf("    结果: %s%n", tamperedClaims == null ? "✗ 验证失败（签名不匹配）" : "✓ 通过");

            // 为普通用户生成 Token
            String userToken = JwtUtil.generateToken("user", "USER");
            System.out.printf("%n  user Token: %s%n", userToken);

            System.out.println("""
                    
                    ========== HTTP 接口测试指南 ==========
                    
                    1. 登录获取 Token:
                       POST http://localhost:8080/api/demo10/auth/login
                       Body: {"username":"admin","password":"admin123"}
                    
                    2. 访问管理员接口（需要 ROLE_ADMIN）:
                       GET http://localhost:8080/api/demo10/admin/dashboard
                       Header: Authorization: Bearer <token>
                    
                    3. 访问内容管理（需要 ROLE_ADMIN 或 ROLE_EDITOR）:
                       GET http://localhost:8080/api/demo10/content/manage
                       Header: Authorization: Bearer <token>
                    
                    4. 查看当前用户:
                       GET http://localhost:8080/api/demo10/user/me
                       Header: Authorization: Bearer <token>
                    
                    5. 数据级权限（user 查看自己的资料 → OK / 查看 admin → 403）:
                       GET http://localhost:8080/api/demo10/user/user/profile  ← OK
                       GET http://localhost:8080/api/demo10/user/admin/profile ← 403
                    
                    【面试考点速记】
                    1. Lambda DSL → 编译时类型安全，Spring Security 6 推荐方式
                    2. JWT 无状态 → 服务端不存储会话，适合微服务和分布式系统
                    3. @PreAuthorize → 方法级权限控制，支持 SpEL 表达式
                    4. SecurityContext → 虚拟线程下使用 INHERITABLETHREADLOCAL 策略
                    5. UserDetailsService → 认证核心接口，从数据源加载用户信息
                    """);
        };
    }
}
