package org.example.aishop.common.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码：200=成功，其它=失败")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "返回数据")
    private T data;

    public Result() {
    }

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMessage(), data);
    }
     public static <T> Result<T> success(String message) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), message);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResponseCodeEnum.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> fail() {
        return new Result<>(ResponseCodeEnum.FAIL.getCode(), ResponseCodeEnum.FAIL.getMessage());
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(ResponseCodeEnum.FAIL.getCode(), message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message);
    }

    public static <T> Result<T> fail(ResponseCodeEnum responseCodeEnum) {
        return new Result<>(responseCodeEnum.getCode(), responseCodeEnum.getMessage());
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
