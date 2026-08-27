package org.example.aishop.common.exception;

import org.example.aishop.common.result.ResponseCodeEnum;

public class UnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public UnauthorizedException() {
        super("未授权访问");
        this.code = ResponseCodeEnum.UNAUTHORIZED.getCode();
    }

    public UnauthorizedException(String message) {
        super(message);
        this.code = ResponseCodeEnum.UNAUTHORIZED.getCode();
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCodeEnum.UNAUTHORIZED.getCode();
    }

    public Integer getCode() {
        return code;
    }
}