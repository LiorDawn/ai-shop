package org.example.aishop.common.exception;

import org.example.aishop.common.result.ResponseCodeEnum;

public class ForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Integer code;

    public ForbiddenException() {
        super("禁止访问");
        this.code = ResponseCodeEnum.FORBIDDEN.getCode();
    }

    public ForbiddenException(String message) {
        super(message);
        this.code = ResponseCodeEnum.FORBIDDEN.getCode();
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCodeEnum.FORBIDDEN.getCode();
    }

    public Integer getCode() {
        return code;
    }
}