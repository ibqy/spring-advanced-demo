package com.ibqy.spring.advanced.demo09_actuator.endpoint;

import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AppInfo 端点的 Web 扩展 —— 为 Web 请求提供增强功能
 *
 * <h3>什么是 EndpointWebExtension？</h3>
 * <p>{@code @EndpointWebExtension} 允许在不修改基础端点的前提下，
 * 为 Web（HTTP）访问增加额外的功能：
 * <ul>
 *     <li>添加额外的 HTTP 响应头</li>
 *     <li>根据请求上下文做不同的处理</li>
 *     <li>自定义 HTTP 状态码</li>
 *     <li>添加 Web 特有的操作</li>
 * </ul>
 *
 * <h3>为什么需要 WebExtension？</h3>
 * <p>基础 {@code @Endpoint} 是协议无关的（同时支持 JMX 和 HTTP）。
 * 如果需要 HTTP 特有的功能（如响应头、状态码），就用 WebExtension 扩展。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>WebExtension 和 @RestController 的区别？→ WebExtension 是对已有端点的增强，
 *     @RestController 是完全独立的端点</li>
 *     <li>一个端点可以有多个 WebExtension 吗？→ 不行，每个端点只能有一个 WebExtension</li>
 *     <li>WebExtension 可以覆盖基础端点的操作吗？→ 可以，方法签名匹配时会优先使用扩展版本</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 * @see AppInfoEndpoint
 */
@Component
@EndpointWebExtension(endpoint = AppInfoEndpoint.class)
public class AppInfoEndpointWebExtension {

    private final AppInfoEndpoint appInfoEndpoint;

    public AppInfoEndpointWebExtension(AppInfoEndpoint appInfoEndpoint) {
        this.appInfoEndpoint = appInfoEndpoint;
    }

    /**
     * 增强版的读取操作 —— 添加额外的 Web 响应信息
     *
     * <p>当通过 HTTP 访问 GET /actuator/appInfo 时，
     * 这个方法会替代基础端点的 {@code @ReadOperation} 被调用。
     *
     * <p>返回 {@link WebEndpointResponse} 可以控制 HTTP 状态码和响应头。
     *
     * @param namespace 当前 Web 服务器的命名空间（区分主服务器和管理服务器）
     * @return 增强的响应
     */
    @ReadOperation
    public WebEndpointResponse<Map<String, Object>> getAppInfo(WebServerNamespace namespace) {
        // 调用基础端点获取数据
        Map<String, Object> baseInfo = appInfoEndpoint.getAppInfo();

        // 添加 Web 特有的信息
        Map<String, Object> webEnhanced = new LinkedHashMap<>(baseInfo);

        // 添加 Web 扩展信息
        Map<String, Object> webExtension = new LinkedHashMap<>();
        webExtension.put("namespace", namespace.getValue());
        webExtension.put("servedVia", "HTTP (WebExtension)");
        webExtension.put("enhancedAt", LocalDateTime.now().toString());
        webExtension.put("hint", "这个响应包含了 WebExtension 添加的额外信息");
        webEnhanced.put("_webExtension", webExtension);

        // 返回自定义状态码 200 + 增强数据
        return new WebEndpointResponse<>(webEnhanced, WebEndpointResponse.STATUS_OK);
    }

    /**
     * 新增 Web 特有操作：获取所有配置（基础端点没有这个操作）
     *
     * <p>对应请求：GET /actuator/appInfo/configs（通过自定义路由实现）
     *
     * @return 所有配置项
     */
    @ReadOperation
    public WebEndpointResponse<Map<String, String>> getAllConfigs() {
        Map<String, String> configs = appInfoEndpoint.getInternalConfig();

        if (configs.isEmpty()) {
            return new WebEndpointResponse<>(Map.of("message", "没有配置项"), WebEndpointResponse.STATUS_OK);
        }

        return new WebEndpointResponse<>(configs, WebEndpointResponse.STATUS_OK);
    }
}
