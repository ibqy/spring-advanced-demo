# 07 · Spring MVC 进阶

> 拦截器、全局异常处理、SSE 推送、自定义消息转换器 —— 打造企业级 Web 层。

## 概述

Spring MVC 是最常用的 Web 框架，但很多开发者只停留在 `@RestController` + `@RequestMapping` 的层面。本 Demo 深入展示 Spring MVC 的高级特性，包括拦截器链、统一异常处理、Server-Sent Events 实时推送、以及自定义消息转换器。

## 核心概念

### Spring MVC 请求处理流程

```
客户端请求
    │
    ▼
┌───────────────┐
│  DispatcherServlet │ ◄── 前端控制器
└───────┬───────┘
        │
    ┌───▼───┐
    │ 拦截器1 │ ◄── HandlerInterceptor.preHandle()
    │ 拦截器2 │
    │ 拦截器N │
    └───┬───┘
        │
    ┌───▼──────────┐
    │ HandlerMapping │ ◄── 查找 Controller
    └───┬──────────┘
        │
    ┌───▼──────────┐
    │HandlerAdapter  │ ◄── 调用 Controller 方法
    └───┬──────────┘
        │
    ┌───▼──────────────┐
    │ HttpMessageConverter│ ◄── 序列化响应体
    └───┬──────────────┘
        │
    ┌───▼───┐
    │ 拦截器 │ ◄── HandlerInterceptor.postHandle()
    └───┬───┘
        │
        ▼
    客户端响应
```

## 代码走读

### 1. 拦截器

```java
// demo06/mvc/interceptor/RequestLoggingInterceptor.java
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        log.info("→ {} {} from {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;
        log.info("← {} {} [{}] in {}ms",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (ex != null) {
            log.error("Request {} completed with error: {}",
                    request.getRequestURI(), ex.getMessage());
        }
    }
}
```

### 2. 注册拦截器

```java
// demo06/mvc/config/WebMvcConfig.java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/h2-console/**");
    }
}
```

### 3. 全局异常处理

```java
// demo06/mvc/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", errors));
    }

    // 兜底异常
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "服务器内部错误"));
    }
}

// 统一错误响应体
public record ErrorResponse(String code, Object message) {}
```

### 4. SSE 实时推送

```java
// demo06/mvc/controller/SseController.java
@RestController
@RequestMapping("/demo06/sse")
public class SseController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(60_000L); // 60秒超时

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        emitters.add(emitter);
        return emitter;
    }

    @PostMapping("/publish")
    public void publish(@RequestBody String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }
}
```

### 5. 自定义消息转换器

```java
// demo06/mvc/converter/CsvMessageConverter.java
public class CsvMessageConverter extends AbstractHttpMessageConverter<List<?>> {

    public static final MediaType TEXT_CSV = new MediaType("text", "csv");

    public CsvMessageConverter() {
        super(TEXT_CSV, Charset.forName("UTF-8"));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @Override
    protected void writeInternal(List<?> list, HttpOutputMessage output) throws IOException {
        StringBuilder sb = new StringBuilder();

        if (!list.isEmpty() && list.getFirst() instanceof Map<?, ?> first) {
            sb.append(String.join(",", first.keySet().stream().map(Object::toString).toList()))
              .append("\n");
        }

        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                sb.append(String.join(",", map.values().stream().map(Object::toString).toList()))
                  .append("\n");
            }
        }

        output.getBody().write(sb.toString().getBytes(getDefaultCharset()));
    }
}
```

## 注解与 API 参考

| 注解 / API | 说明 |
|-----------|------|
| `HandlerInterceptor` | MVC 拦截器接口（preHandle/postHandle/afterCompletion） |
| `@RestControllerAdvice` | 全局异常处理 + 响应体增强 |
| `@ExceptionHandler` | 特定异常类型的处理方法 |
| `SseEmitter` | SSE 服务端推送 |
| `HttpMessageConverter` | 消息转换器接口 |
| `WebMvcConfigurer` | MVC 配置接口 |
| `@ResponseStatus` | 指定 HTTP 状态码 |

## 面试考点

::: warning 高频面试题
1. **拦截器和过滤器的区别？**
   - Filter 是 Servlet 规范，工作在 Servlet 容器层
   - HandlerInterceptor 是 Spring MVC 规范，工作在 DispatcherServlet 内部
   - Interceptor 可以访问 Handler、ModelAndView 等 Spring 对象
   - 执行顺序：Filter → Interceptor.preHandle → Controller → Interceptor.postHandle → Interceptor.afterCompletion

2. **SSE 和 WebSocket 的区别？**
   - SSE：单向通信（服务器 → 客户端），基于 HTTP，自动重连
   - WebSocket：双向通信，独立协议（ws://），需要握手升级
   - SSE 更简单，适合通知、推送、实时数据流场景

3. **@ControllerAdvice 和 @RestControllerAdvice？**
   - `@ControllerAdvice` = `@Controller` + `@ResponseBody`（所有方法返回 JSON）
   - `@RestControllerAdvice` 是它的语义化别名（Spring 4.3+）
:::

## 常见陷阱

::: danger 陷阱 1：SSE 连接泄漏
SseEmitter 使用后必须清理，否则线程和连接资源会泄漏。务必注册 `onCompletion`、`onTimeout`、`onError` 回调。
:::

::: danger 陷阱 2：异常处理器顺序
多个 `@ExceptionHandler` 匹配同一异常类型时，选择最精确的。确保 `Exception.class` 的兜底方法在最后。
:::

::: danger 陷阱 3：消息转换器冲突
自定义 `HttpMessageConverter` 的 MediaType 可能与已有的冲突。通过 `WebMvcConfigurer.configureMessageConverters()` 替换，或 `extendMessageConverters()` 追加。
:::

## 延伸阅读

- [Spring MVC 文档](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Server-Sent Events 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [HttpMessageConverter 深入](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/message-conversion.html)
