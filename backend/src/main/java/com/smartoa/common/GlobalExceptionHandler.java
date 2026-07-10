package com.smartoa.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 将 BusinessException.code 解析为真实 HTTP 状态码。
     * <p>
     * 规则：
     * - 1002 → 401（登录失败）
     * - 有效 HTTP 错误状态码（4xx/5xx）→ 直接使用
     * - 其他自定义码 → 422
     */
    private HttpStatus resolveHttpStatus(int code) {
        if (code == 1002) {
            return HttpStatus.UNAUTHORIZED;
        }
        HttpStatus status = HttpStatus.resolve(code);
        if (status != null && status.isError()) {
            return status;
        }
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        HttpStatus httpStatus = resolveHttpStatus(e.getCode());
        log.warn("业务异常: httpStatus={} code={} message={}", httpStatus.value(), e.getCode(), e.getMessage());
        return ResponseEntity.status(httpStatus).body(Result.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(500).body(Result.error(500, "服务器内部错误"));
    }
}
