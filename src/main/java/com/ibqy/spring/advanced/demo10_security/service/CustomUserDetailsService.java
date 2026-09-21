package com.ibqy.spring.advanced.demo10_security.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义 UserDetailsService —— 从自定义数据源加载用户信息
 *
 * <h3>UserDetailsService 在 Spring Security 中的位置</h3>
 * <pre>
 *   认证流程：
 *   1. 用户提交 username + password
 *   2. DaoAuthenticationProvider 接收认证请求
 *   3. 调用 UserDetailsService.loadUserByUsername(username)
 *   4. 返回 UserDetails 对象
 *   5. PasswordEncoder 比对密码
 *   6. 认证成功 → 创建 SecurityContext
 * </pre>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>UserDetailsService vs AuthenticationProvider？→ UserDetailsService 只负责加载用户信息，
 *     AuthenticationProvider 负责完整的认证逻辑（包括密码比对）</li>
 *     <li>实际项目中 loadUserByUsername 查哪里？→ 通常查数据库（User 表），也可以查 LDAP、OAuth 等</li>
 *     <li>UserDetails 接口包含什么？→ username、password、authorities（权限集合）、
 *     isAccountNonExpired/Locked/CredentialsNonExpired/Enabled</li>
 *     <li>密码要不要加密存储？→ 必须！使用 BCryptPasswordEncoder 或 Argon2PasswordEncoder</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * 模拟用户数据库
     * <p>实际项目中这里应该是 UserRepository 或 JdbcTemplate
     *
     * <p>密码使用 {noop} 前缀表示明文存储（仅用于教学！）
     * <p>生产环境必须使用 BCrypt 加密！
     */
    private final Map<String, UserRecord> userDatabase = new ConcurrentHashMap<>();

    public CustomUserDetailsService() {
        // 初始化模拟用户数据
        userDatabase.put("admin", new UserRecord(
                "admin", "{noop}admin123", "管理员",
                List.of("ROLE_ADMIN", "ROLE_USER")
        ));
        userDatabase.put("user", new UserRecord(
                "user", "{noop}user123", "普通用户",
                List.of("ROLE_USER")
        ));
        userDatabase.put("editor", new UserRecord(
                "editor", "{noop}editor123", "编辑",
                List.of("ROLE_EDITOR", "ROLE_USER")
        ));
    }

    /**
     * 根据用户名加载用户详情
     *
     * <p>这是 UserDetailsService 的核心方法。
     * Spring Security 的 DaoAuthenticationProvider 会调用这个方法来获取用户信息。
     *
     * @param username 用户名
     * @return UserDetails 对象（Spring Security 用它进行认证和授权）
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRecord record = userDatabase.get(username);

        if (record == null) {
            System.out.printf("  [UserDetailsService] 用户 '%s' 不存在！%n", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        System.out.printf("  [UserDetailsService] 加载用户: %s, 角色: %s%n",
                username, record.roles);

        // 构建 Spring Security 的 UserDetails 对象
        return User.builder()
                .username(record.username)
                .password(record.password) // {noop} 前缀表示明文，{bcrypt} 表示 BCrypt 加密
                .authorities(
                        record.roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                )
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * 内部用户记录类（模拟数据库实体）
     */
    private record UserRecord(
            String username,
            String password,
            String displayName,
            List<String> roles
    ) {
    }
}
