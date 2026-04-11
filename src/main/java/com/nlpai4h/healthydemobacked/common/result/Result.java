package com.nlpai4h.healthydemobacked.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 后端统一返回结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private Integer code;   // 状态码
    private String msg;     // 提示信息
    private T data;         // 数据

    /**
     * 成功返回
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    /**
     * 成功返回（带数据和消息）
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), msg, data);
    }

    /**
     * 失败返回（默认系统错误）
     */
    public static <T> Result<T> error() {
        return new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg(), null);
    }

    /**
     * 失败返回（自定义消息，默认系统错误码）
     * 兼容旧代码 Result.error(String msg)
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(ErrorCode.SYSTEM_ERROR.getCode(), msg, null);
    }

    /**
     * 失败返回（指定错误码）
     */
    public static <T> Result<T> error(IErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    /**
     * 失败返回（指定错误码和自定义消息）
     */
    public static <T> Result<T> error(IErrorCode errorCode, String msg) {
        return new Result<>(errorCode.getCode(), msg, null);
    }
}
