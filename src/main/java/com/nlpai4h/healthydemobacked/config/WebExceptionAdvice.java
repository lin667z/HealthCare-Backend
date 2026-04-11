package com.nlpai4h.healthydemobacked.config;

import com.nlpai4h.healthydemobacked.common.exception.BaseException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Optional;

/**
 * 全局异常处理器
 * 统一处理Controller层抛出的异常，返回统一的错误响应格式
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 处理自定义基础异常（包括 BusinessException 和 AuthException）
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Result<Object>> handleBaseException(BaseException e) {
        log.warn("自定义异常 [{}]: code={}, msg={}", e.getClass().getSimpleName(), e.getCode(), e.getMsg());
        return response(new Result<>(e.getCode(), e.getMsg(), null), e.getHttpStatus());
    }

    /**
     * 处理参数校验异常 (MethodArgumentNotValidException)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = extractBindingResultMsg(e.getBindingResult());
        log.warn("参数校验异常: {}", msg);
        return response(Result.error(ErrorCode.PARAM_ERROR, msg));
    }

    /**
     * 处理参数绑定异常 (BindException)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Object>> handleBindException(BindException e) {
        String msg = extractBindingResultMsg(e.getBindingResult());
        log.warn("参数绑定异常: {}", msg);
        return response(Result.error(ErrorCode.PARAM_ERROR, msg));
    }

    /**
     * 处理参数非法异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数非法异常: {}", e.getMessage());
        return response(Result.error(ErrorCode.PARAM_ERROR, e.getMessage()));
    }

    /**
     * 处理数据库唯一键冲突异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Object>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据库唯一键冲突: {}", e.getMessage());
        return response(Result.error(ErrorCode.DATA_ALREADY_EXIST, "数据已存在，请勿重复操作"));
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("路径不存在: {}", e.getRequestURL());
        return response(Result.error(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不允许: {}", e.getMessage());
        return response(Result.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getMessage());
        return response(Result.error(ErrorCode.PARAM_ERROR, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return response(Result.error(ErrorCode.PARAM_ERROR, "参数类型不匹配"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return response(Result.error(ErrorCode.PARAM_ERROR, "请求体解析失败"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Object>> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = Optional.ofNullable(e.getConstraintViolations())
                .flatMap(vs -> vs.stream().findFirst())
                .map(v -> compactPath(v.getPropertyPath() != null ? v.getPropertyPath().toString() : null) + ": " + v.getMessage())
                .orElse("参数校验失败");
        log.warn("参数校验异常: {}", msg);
        return response(Result.error(ErrorCode.PARAM_ERROR, msg));
    }

    /**
     * 处理安全异常
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Result<Object>> handleSecurityException(SecurityException e) {
        log.warn("安全异常: {}", e.getMessage());
        return response(Result.error(ErrorCode.FORBIDDEN, e.getMessage()));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Result<Object>> handleServletRequestBindingException(ServletRequestBindingException e) {
        log.warn("请求参数绑定失败: {}", e.getMessage());
        return response(Result.error(ErrorCode.PARAM_ERROR, e.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Object>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("不支持的Content-Type: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Result.error(ErrorCode.PARAM_ERROR, "不支持的Content-Type"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Object>> handleHttpMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.warn("无法接受的响应类型: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Result.error(ErrorCode.PARAM_ERROR, "无法接受的响应类型"));
    }

    /**
     * 处理其他运行时异常（兜底处理）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.SYSTEM_ERROR, "服务器异常，请稍后重试"));
    }

    /**
     * 处理所有其他异常（最终兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.SYSTEM_ERROR, "系统异常，请联系管理员"));
    }

    private static ResponseEntity<Result<Object>> response(Result<Object> body) {
        int code = body.getCode() != null ? body.getCode() : ErrorCode.SYSTEM_ERROR.getCode();
        return response(body, toHttpStatus(code).value());
    }

    private static ResponseEntity<Result<Object>> response(Result<Object> body, int httpStatus) {
        HttpStatus status = HttpStatus.resolve(httpStatus);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(body);
    }

    private static HttpStatus toHttpStatus(int code) {
        if (code >= 400 && code <= 599) {
            return HttpStatus.resolve(code) != null ? HttpStatus.resolve(code) : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    private static String extractBindingResultMsg(BindingResult bindingResult) {
        if (bindingResult == null) {
            return "参数校验失败";
        }
        if (bindingResult.hasFieldErrors()) {
            FieldError fe = bindingResult.getFieldErrors().get(0);
            String field = fe.getField();
            String msg = fe.getDefaultMessage();
            if (msg == null || msg.isBlank()) {
                msg = "参数不合法";
            }
            return field != null && !field.isBlank() ? field + ": " + msg : msg;
        }
        if (bindingResult.hasGlobalErrors()) {
            ObjectError oe = bindingResult.getGlobalErrors().get(0);
            String msg = oe.getDefaultMessage();
            return msg != null && !msg.isBlank() ? msg : "参数校验失败";
        }
        String msg = bindingResult.getAllErrors().stream()
                .findFirst()
                .map(ObjectError::getDefaultMessage)
                .orElse("参数校验失败");
        return msg != null && !msg.isBlank() ? msg : "参数校验失败";
    }

    private static String compactPath(String path) {
        if (path == null || path.isBlank()) {
            return "param";
        }
        int idx = path.lastIndexOf('.');
        return idx >= 0 && idx + 1 < path.length() ? path.substring(idx + 1) : path;
    }
}
