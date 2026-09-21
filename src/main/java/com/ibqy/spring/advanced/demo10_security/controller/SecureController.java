package com.ibqy.spring.advanced.demo10_security.controller;

import com.ibqy.spring.advanced.demo10_security.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安全控制器 —— 演示方法级别的安全注解
 *
 * <h2>方法安全注解对比</h2>
 * <table>
 *     <tr><th>注解</th><th>检查时机</th><th>用途</th></tr>
 *     <tr><td>@PreAuthorize</td><td>方法执行前</td><td>基于参数/条件判断是否允许访问</td></tr>
 *     <tr><td>@PostAuthorize</td><td>方法执行后</td><td>基于返回值判断是否允许返回结果</td></tr>
 *     <tr><td>@PreFilter</td><td>方法执行前</td><td>过滤集合参数中的元素</td></tr>
 *     <tr><td>@PostFilter</td><td>方法执行后</td><td>过滤返回集合中的元素</td></tr>
 * </table>
 *
 * <h2>SpEL 表达式</h2>
 * <pre>
 *   hasRole('ADMIN')          → 当前用户是否有 ADMIN 角色
 *   hasAnyRole('A','B')       → 是否有 A 或 B 角色
 *   #username                 → 方法参数 username 的值
 *   returnObject              → 方法返回值（@PostAuthorize 中使用）
 *   authentication.name       → 当前认证的用户名
 * </pre>
 *
 * <h2>面试考点</h2>
 * <ul>
 *     <li>@PreAuthorize 和 URL 级别授权的区别？→ 方法级别更灵活（可以用方法参数），
 *     URL 级别更集中（一个地方配置所有规则）</li>
 *     <li>@PreAuthorize 中的 SpEL 能调用 Bean 方法吗？→ 可以！
 *     @PreAuthorize("@myBean.myMethod(#param)") 用 @ 前缀引用 Bean</li>
 *     <li>@PostAuthorize 的典型场景？→ 数据级别的权限控制（如：只能查看自己的订单）</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see SecurityConfig
 * @see JwtUtil
 */
@RestController
@RequestMapping("/api/demo10")
public class SecureController {

    private static final Logger log = LoggerFactory.getLogger(SecureController.class);

    private final AuthenticationManager authenticationManager;

    public SecureController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // ====================== 登录接口（公开） ======================

    /**
     * 用户登录 —— 返回 JWT Token
     *
     * <p>流程：
     * <ol>
     *     <li>接收用户名和密码</li>
     *     <li>使用 AuthenticationManager 验证凭证</li>
     *     <li>认证通过后生成 JWT Token</li>
     *     <li>返回 Token 给客户端</li>
     * </ol>
     *
     * <p>测试：
     * <pre>
     * curl -X POST http://localhost:8080/api/demo10/auth/login \
     *   -H "Content-Type: application/json" \
     *   -d '{"username":"admin","password":"admin123"}'
     * </pre>
     */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        System.out.printf("[SecureController] 登录请求: 用户=%s%n", username);

        try {
            // 1. 使用 AuthenticationManager 进行认证
            // 内部流程：UserDetailsService.loadUserByUsername → PasswordEncoder.matches
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            // 2. 认证成功，从认证信息中提取角色
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("ROLE_USER");
            String cleanRole = role.replace("ROLE_", "");

            // 3. 生成 JWT Token
            String token = JwtUtil.generateToken(username, cleanRole);

            // 4. 构建响应
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("username", username);
            response.put("role", cleanRole);
            response.put("expiresIn", 3600);
            response.put("message", "登录成功！请在后续请求中使用 Authorization: Bearer <token>");

            System.out.printf("[SecureController] 用户 '%s' 登录成功，已签发 JWT%n", username);
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            System.out.printf("[SecureController] 用户 '%s' 登录失败: %s%n", username, e.getMessage());

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "认证失败");
            error.put("message", "用户名或密码错误");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    // ====================== 公开接口（无需认证） ======================

    /**
     * 公开信息 —— 不需要认证
     */
    @GetMapping("/public/info")
    public Map<String, Object> publicInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("message", "这是公开信息，无需认证即可访问");
        info.put("hint", "请使用 /api/demo10/auth/login 获取 Token");
        info.put("timestamp", LocalDateTime.now().toString());
        return info;
    }

    // ====================== 受保护接口（需要认证） ======================

    /**
     * 查看当前用户信息 —— 需要认证
     *
     * <p>使用 {@code @AuthenticationPrincipal} 注解直接注入当前用户对象。
     */
    @GetMapping("/user/me")
    public Map<String, Object> currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("username", userDetails.getUsername());
        info.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList());
        info.put("accountNonExpired", userDetails.isAccountNonExpired());
        info.put("accountNonLocked", userDetails.isAccountNonLocked());

        // 演示 SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        info.put("authClass", auth.getClass().getSimpleName());
        info.put("hint", "这些信息来自 @AuthenticationPrincipal 和 SecurityContextHolder");

        return info;
    }

    // ====================== @PreAuthorize 方法安全 ======================

    /**
     * 管理员专属接口 —— 只有 ROLE_ADMIN 角色可以访问
     *
     * <p>{@code @PreAuthorize} 在方法执行前检查权限。
     * 如果当前用户没有 ROLE_ADMIN，会抛出 AccessDeniedException（403）。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public Map<String, Object> adminDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "欢迎进入管理员面板！");
        data.put("data", Map.of(
                "totalUsers", 1024,
                "activeOrders", 56,
                "systemLoad", "42%"
        ));
        return data;
    }

    /**
     * 多角色访问 —— ADMIN 或 EDITOR 都可以
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/content/manage")
    public Map<String, Object> contentManagement() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "内容管理面板（ADMIN 和 EDITOR 均可访问）");
        data.put("actions", new String[]{"create", "edit", "publish", "archive"});
        return data;
    }

    /**
     * 基于参数的权限检查 —— 只能查看自己的数据
     *
     * <p>SpEL 中 {@code #targetUser} 引用方法参数。
     * <p>{@code authentication.name} 获取当前认证的用户名。
     *
     * <p>面试考点：这就是"数据级别权限控制"（Data-level Security），
     * 不仅控制"谁能访问这个接口"，还控制"谁能访问哪条数据"。
     */
    @PreAuthorize("authentication.name == #targetUser or hasRole('ADMIN')")
    @GetMapping("/user/{targetUser}/profile")
    public Map<String, Object> userProfile(@PathVariable String targetUser) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", targetUser);
        profile.put("email", targetUser + "@example.com");
        profile.put("level", "VIP");
        profile.put("message", "你有权查看此用户的资料（是自己的资料或者是管理员）");
        return profile;
    }

    // ====================== @PostAuthorize 方法安全 ======================

    /**
     * 方法执行后再检查权限
     *
     * <p>{@code @PostAuthorize} 的典型场景：
     * 方法正常执行了，但根据返回值判断是否应该将结果返回给调用者。
     *
     * <p>{@code returnObject} 是内置变量，代表方法的返回值。
     *
     * <p>面试考点：@PostAuthorize 和 @PreAuthorize 的选择？
     * → 如果能提前判断就用 @PreAuthorize（性能更好，方法不会执行）
     * → 如果需要根据返回结果判断就用 @PostAuthorize
     */
    @PostAuthorize("returnObject.get('owner') == authentication.name or hasRole('ADMIN')")
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getOrder(@PathVariable String orderId) {
        // 模拟订单数据
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", orderId);
        order.put("amount", 299.99);
        order.put("owner", "user"); // 订单所属用户
        order.put("status", "COMPLETED");
        order.put("hint", "只有订单所有者或管理员才能看到这个结果（@PostAuthorize 控制）");
        return order;
    }
}
