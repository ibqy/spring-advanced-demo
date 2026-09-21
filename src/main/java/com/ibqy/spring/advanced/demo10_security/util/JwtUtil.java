package com.ibqy.spring.advanced.demo10_security.util;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * JWT 工具类 —— 纯 Java 实现 HMAC-SHA256 签名（不依赖第三方 JWT 库）
 *
 * <h3>JWT 结构</h3>
 * <pre>
 *   JWT = Header.Payload.Signature
 *
 *   Header:  {"alg": "HS256", "typ": "JWT"}          → Base64URL 编码
 *   Payload: {"sub": "admin", "iat": 1726900000, ...}  → Base64URL 编码
 *   Signature: HMAC-SHA256(Header.Payload, Secret)      → Base64URL 编码
 * </pre>
 *
 * <h3>为什么不使用 jjwt 等第三方库？</h3>
 * <p>教学目的：让同学理解 JWT 的底层原理。
 * 生产环境中推荐使用 Spring Security 的 OAuth2 Resource Server 或 jjwt 库。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>JWT 和 Session 的区别？→ JWT 无状态（服务端不存储），Session 有状态（服务端存储）</li>
 *     <li>JWT 的 Payload 是加密的吗？→ 不是！Base64URL 只是编码不是加密。
 *     敏感信息不要放在 Payload 中</li>
 *     <li>JWT 如何吊销？→ 无法主动吊销（只能等过期）。解决方案：黑名单机制 + 短有效期 + Refresh Token</li>
 *     <li>HS256 vs RS256？→ HS256 对称加密（同一密钥签名验证），
 *     RS256 非对称加密（私钥签名、公钥验证，适合分布式系统）</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
public final class JwtUtil {

    /**
     * 密钥 —— 必须至少 256 位（32 字节）用于 HMAC-SHA256
     *
     * <p>面试考点：生产环境中密钥应该从配置中心或环境变量读取，绝不能硬编码！
     */
    private static final String SECRET_STRING =
            "ibqy-spring-advanced-demo-jwt-secret-key-2026!";

    /**
     * JWT 密钥对象（线程安全，可复用）
     */
    private static final SecretKey SECRET_KEY = new SecretKeySpec(
            SECRET_STRING.getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
    );

    /**
     * 默认过期时间：1 小时
     */
    private static final long DEFAULT_EXPIRATION_SECONDS = 3600;

    private JwtUtil() {
        // 工具类禁止实例化
    }

    /**
     * 生成 JWT Token
     *
     * @param subject 用户标识（通常是用户名或用户 ID）
     * @param role    用户角色
     * @return 完整的 JWT 字符串（Header.Payload.Signature）
     */
    public static String generateToken(String subject, String role) {
        return generateToken(subject, role, DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * 生成带自定义过期时间的 JWT Token
     *
     * @param subject           用户标识
     * @param role              用户角色
     * @param expirationSeconds 过期时间（秒）
     * @return JWT 字符串
     */
    public static String generateToken(String subject, String role, long expirationSeconds) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;

        // 1. 构建 Header
        String header = base64UrlEncode(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
        );

        // 2. 构建 Payload（手动拼接 JSON，避免引入 Jackson 依赖）
        String payload = base64UrlEncode(
                "{\"sub\":\"" + subject + "\","
                        + "\"role\":\"" + role + "\","
                        + "\"iat\":" + now + ","
                        + "\"exp\":" + exp + "}"
        );

        // 3. 计算签名
        String signatureInput = header + "." + payload;
        String signature = base64UrlEncode(hmacSha256(signatureInput));

        // 4. 拼接最终 JWT
        return header + "." + payload + "." + signature;
    }

    /**
     * 验证并解析 JWT Token
     *
     * @param token JWT 字符串
     * @return 解析结果（包含 sub、role、exp 等信息），验证失败返回 null
     */
    public static JwtClaims validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            // 1. 分割 JWT 为三部分
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.out.println("  [JWT验证] 格式错误：JWT 必须包含 3 部分");
                return null;
            }

            String header = parts[0];
            String payload = parts[1];
            String providedSignature = parts[2];

            // 2. 重新计算签名
            String signatureInput = header + "." + payload;
            String expectedSignature = base64UrlEncode(hmacSha256(signatureInput));

            // 3. 对比签名（使用常量时间比较，防止时序攻击）
            if (!constantTimeEquals(providedSignature, expectedSignature)) {
                System.out.println("  [JWT验证] 签名验证失败！");
                return null;
            }

            // 4. 解析 Payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            System.out.println("  [JWT验证] Payload: " + payloadJson);

            // 5. 提取字段并检查过期
            String sub = extractJsonField(payloadJson, "sub");
            String role = extractJsonField(payloadJson, "role");
            long exp = Long.parseLong(extractJsonField(payloadJson, "exp"));

            if (Instant.now().getEpochSecond() > exp) {
                System.out.println("  [JWT验证] Token 已过期！");
                return null;
            }

            return new JwtClaims(sub, role, exp);

        } catch (Exception e) {
            System.out.println("  [JWT验证] 解析异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取密钥信息（教学用）
     */
    public static String getSecretKeyInfo() {
        return "算法: %s, 密钥长度: %d 位".formatted(
                SECRET_KEY.getAlgorithm(),
                SECRET_KEY.getEncoded().length * 8
        );
    }

    // ====================== 内部工具方法 ======================

    /**
     * HMAC-SHA256 签名
     */
    private static byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(SECRET_KEY);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 签名失败", e);
        }
    }

    /**
     * Base64 URL 安全编码
     */
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 常量时间字符串比较（防止时序攻击）
     *
     * <p>面试考点：为什么不能用 equals()？
     * → String.equals() 在第一个不同字符处就返回，攻击者可以通过测量响应时间逐位猜测签名。
     * → 常量时间比较无论在哪一位不同，耗时都相同。
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 从 JSON 字符串中提取字段值（简单实现，避免引入 Jackson）
     */
    private static String extractJsonField(String json, String field) {
        String searchKey = "\"" + field + "\":";
        int start = json.indexOf(searchKey);
        if (start < 0) return null;

        start += searchKey.length();
        // 跳过空格
        while (start < json.length() && json.charAt(start) == ' ') start++;

        if (json.charAt(start) == '"') {
            // 字符串值
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        } else {
            // 数字值
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }

    // ====================== 内部类：JWT Claims ======================

    /**
     * JWT 解析结果
     */
    public record JwtClaims(String subject, String role, long expiration) {

        public String expirationFormatted() {
            return Instant.ofEpochSecond(expiration).toString();
        }
    }
}
