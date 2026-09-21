package com.ibqy.spring.advanced.demo06_mvc_advanced.exception;

/**
 * 自定义业务异常
 *
 * <p>在实际项目中，业务异常和系统异常应该分开处理。
 * 业务异常通常包含一个错误码和可读的错误信息，用于向前端返回结构化的错误响应。
 *
 * <h3>面试考点</h3>
 * <ul>
 *     <li>为什么不用 RuntimeException 直接抛出？→ 因为需要携带错误码，便于前端判断处理</li>
 *     <li>为什么要继承 RuntimeException 而不是 Exception？→ Spring 的事务默认只对 RuntimeException 回滚</li>
 *     <li>异常应该在哪一层抛出？→ Service 层抛出，Controller 层不处理业务异常，交给全局异常处理器</li>
 * </ul>
 *
 * @author ibqy
 * @since 2026-09-21
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码（如 1001=用户不存在, 1002=余额不足 等）
     */
    private final int errorCode;

    public BusinessException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
