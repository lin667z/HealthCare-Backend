package com.nlpai4h.healthydemobacked.common.exception;

import com.nlpai4h.healthydemobacked.common.result.IErrorCode;
import lombok.Getter;

/**
 * 业务/基础异常基类
 * 统一所有自定义异常的行为，提供对错误码的完整支持
 */
@Getter
public abstract class BaseException extends RuntimeException implements IErrorCode {

    private final IErrorCode errorCode;
    private final String overrideMsg;

    protected BaseException(IErrorCode errorCode) {
        super(errorCode.getMsg());
        this.errorCode = errorCode;
        this.overrideMsg = null;
    }

    protected BaseException(IErrorCode errorCode, String overrideMsg) {
        super(overrideMsg);
        this.errorCode = errorCode;
        this.overrideMsg = overrideMsg;
    }

    protected BaseException(IErrorCode errorCode, String overrideMsg, Throwable cause) {
        super(overrideMsg, cause);
        this.errorCode = errorCode;
        this.overrideMsg = overrideMsg;
    }

    @Override
    public Integer getCode() {
        return errorCode.getCode();
    }

    @Override
    public String getMsg() {
        return overrideMsg != null ? overrideMsg : errorCode.getMsg();
    }

    @Override
    public int getHttpStatus() {
        return errorCode.getHttpStatus();
    }
}
