package com.nlpai4h.healthydemobacked.common.result;

/**
 * 通用错误码
 */
public enum ErrorCode implements IErrorCode {
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    // 业务错误码 (可根据需要扩展)
    OPERATION_ERROR(50001, "操作失败"),
    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    ACCOUNT_LOCKED(1003, "账号被锁定"),
    DATA_ALREADY_EXIST(1004, "数据已存在");

    private final Integer code;
    private final String msg;

    ErrorCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }
}
