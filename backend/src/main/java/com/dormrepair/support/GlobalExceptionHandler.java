package com.dormrepair.support;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> business(BusinessException ex) {
        HttpStatus status = ex.getCode() == 401 ? HttpStatus.UNAUTHORIZED
                : ex.getCode() == 403 ? HttpStatus.FORBIDDEN
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiResult.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResult<Void>> validation(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof BindException bind) {
            message = bind.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + "：" + error.getDefaultMessage())
                    .collect(java.util.stream.Collectors.joining("；"));
        }
        return ResponseEntity.badRequest().body(ApiResult.fail(400, message));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> denied(Exception ex) {
        return ResponseEntity.status(403).body(ApiResult.fail(403, "当前角色无权访问该功能"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> unknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(500, "服务器内部错误，请联系管理员"));
    }
}
