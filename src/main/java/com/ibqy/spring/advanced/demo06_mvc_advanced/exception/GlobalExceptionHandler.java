package com.ibqy.spring.advanced.demo06_mvc_advanced.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 —— 使用 {@link RestControllerAdvice} 统一处理所有 Controller 异常
 *
 * <p>{@code @RestControllerAdvice = @ControllerAdvice + @ResponseBody}
 * <p>它会被 Spring MVC 的 {@link org.springframework.web.servlet.HandlerExceptionResolver} 机制发现，
 * 当 Controller 抛出异常时，会按 {@code @ExceptionHandler} 声明的类型进行匹配处理。
 *
 * <h3>异常处理优先级（从高到低）</h3>
 * <ol>
 *     <li>精确匹配的 @ExceptionHandler（如 BusinessException.class）</li>
 *     <li>父类匹配的 @ExceptionHandler（如 RuntimeException.class）</li>
 *     <li>兜底的 @ExceptionHandler（如 Exception.class）</li>
 * </ol>
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>@ControllerAdvice vs @RestControllerAdvice？→ 后者自带 @ResponseBody，直接返回 JSON</li>
 *     <li>为什么不用 Filter 处理异常？→ Filter 在 DispatcherServlet 之前，无法感知 Controller 层的异常</li>
 *     <li>异常处理器能拿到请求信息吗？→ 可以，方法参数中可以注入 HttpServletRequest 等</li>
 *     <li>一个项目中可以有多个 @RestControllerAdvice 吗？→ 可以，用 @Order 控制优先级</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
@RestControllerAdvice(basePackages = "com.ibqy.spring.advanced.demo06_mvc_advanced")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ====================== 业务异常处理 ======================

    /**
     * 处理自定义业务异常
     * <p>这是最常见的异常处理场景：Service 层抛出具业务语义的异常，这里统一转换为 HTTP 响应。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("[业务异常] errorCode={}, message={}", ex.getErrorCode(), ex.getMessage());

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST.value(),
                "业务处理异常",
                ex.getMessage(),
                Map.of("errorCode", ex.getErrorCode())
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ====================== 参数校验异常处理 ======================

    /**
     * 处理 @Valid 参数校验失败
     * <p>当 Controller 方法参数使用 @Valid 注解时，校验失败会抛出此异常。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // 提取所有字段的校验错误信息
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "校验失败",
                        (existing, replacement) -> existing // 如果有重复字段，保留第一个
                ));

        log.warn("[参数校验失败] {}", fieldErrors);

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST.value(),
                "参数校验失败",
                fieldErrors.toString(),
                Map.of("fieldErrors", fieldErrors)
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理约束违反异常（如 @RequestParam 上的 @Min/@Max）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("[约束违反] {}", ex.getMessage());

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST.value(),
                "约束违反",
                ex.getMessage(),
                Map.of()
        );
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 处理缺少必需参数的异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("[缺少参数] 参数名: {}, 类型: {}", ex.getParameterName(), ex.getParameterType());

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST.value(),
                "缺少必需参数",
                "缺少参数: " + ex.getParameterName(),
                Map.of()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ====================== 资源异常处理 ======================

    /**
     * 处理 404 资源未找到
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        Map<String, Object> body = buildErrorBody(
                HttpStatus.NOT_FOUND.value(),
                "资源不存在",
                ex.getResourcePath(),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ====================== 兜底处理 ======================

    /**
     * 兜底：处理所有未被上面匹配的异常
     * <p>生产环境中，这里应该记录完整堆栈但只向前端返回友好提示（避免泄漏敏感信息）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("[系统异常] 未预期的错误", ex);

        Map<String, Object> body = buildErrorBody(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务器内部错误",
                "系统繁忙，请稍后再试",
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ====================== 工具方法 ======================

    /**
     * 构建统一的错误响应体
     * <p>返回格式：
     * <pre>
     * {
     *   "timestamp": "2026-09-21T10:00:00",
     *   "status": 400,
     *   "error": "业务处理异常",
     *   "message": "具体错误描述",
     *   "path": "/api/...",        // 由 Spring 自动填充
     *   "details": { ... }         // 附加信息
     * }
     * </pre>
     */
    private Map<String, Object> buildErrorBody(int status, String error, String message,
                                                Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}
