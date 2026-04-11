package com.nlpai4h.healthydemobacked.common.result;

/**
 * 错误码接口
 */
public interface IErrorCode {
    Integer getCode();
    String getMsg();
    
    /**
     * 获取对应的 HTTP 状态码
     * 默认通过业务码推导，通用 4xx/5xx 直接映射，业务码默认返回 200
     */
    default int getHttpStatus() {
        Integer code = getCode();
        if (code != null && code >= 400 && code <= 599) {
            return code;
        }
        return 200;
    }
}
