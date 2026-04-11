package com.nlpai4h.healthydemobacked.common.exception;

import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.common.result.IErrorCode;

/**
 * 业务异常类
 * 用于处理业务逻辑中的异常情况
 */
public class BusinessException extends BaseException {
    
    public BusinessException(String message) {
        super(ErrorCode.SYSTEM_ERROR, message);
    }
    
    public BusinessException(IErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(IErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(ErrorCode.SYSTEM_ERROR, message, cause);
    }
}
