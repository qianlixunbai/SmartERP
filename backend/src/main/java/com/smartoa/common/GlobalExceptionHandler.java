package com.smartoa.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 有效的 HTTP 错误状态码集合。
     * BusinessException.code 在此集合中时，直接作为 HTTP 状态返回；
     * 不在集合中的自定义业务码（如 1002）需单独映射。
     */
    private static final Set<Integer> HTTP_ERROR_CODES = Set.of(
            400, 401, 403, 404, 405, 406, 408, 409, 410, 422, 429, 502, 503
    );

    /**
     * 自定义业务码 → HTTP 状态码映射。
     * BusinessException 中 code=500（无显式 code 构造器默认值）视为业务校验失败 → 422。
     */
    private static int mapCustomCode(int code) {
        return switch (code) {
            case 1002 -> 401; // 用户名或密码错误
            case 500 -> 422;  // 无显式 code 的业务校验失败
            default -> 422;   // 其他业务校验失败
        };
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        int code = e.getCode();
        int httpStatus = HTTP_ERROR_CODES.contains(code) ? code : mapCustomCode(code);
        log.warn("业务异常: httpStatus={} code={} message={}", httpStatus, code, e.getMessage());
        return ResponseEntity.status(httpStatus).body(Result.error(code, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ResponseEntity.status(500).body(Result.error(500, "服务器内部错误"));
    }
}
