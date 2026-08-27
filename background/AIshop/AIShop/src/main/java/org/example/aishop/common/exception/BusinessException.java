package org.example.aishop.common.exception;

import org.example.aishop.common.result.ResponseCodeEnum;

public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Integer code;

    public BusinessException() {
        super();
        this.code = 500;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResponseCodeEnum responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}