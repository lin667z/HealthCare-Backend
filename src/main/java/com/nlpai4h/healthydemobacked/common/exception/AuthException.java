package com.nlpai4h.healthydemobacked.common.exception;

import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.common.result.IErrorCode;

public class AuthException extends BaseException {

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public AuthException(IErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AuthException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}

