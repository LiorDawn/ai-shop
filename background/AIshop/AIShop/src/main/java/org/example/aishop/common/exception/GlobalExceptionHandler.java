package org.example.aishop.common.exception;

import org.example.aishop.common.result.Result;
import org.example.aishop.common.result.ResponseCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        logger.warn("业务异常 [{}]: {}", e.getCode(), e.getMessage());
        HttpStatus status = (e.getCode() != null && e.getCode() == 401) ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 重复提交异常
     */
    @ExceptionHandler(DuplicateSubmitException.class)
    public Result<Void> handleDuplicateSubmitException(DuplicateSubmitException e) {
        logger.warn("重复提交: {}", e.getMessage());
        return Result.fail(429, e.getMessage());
    }

    /**
     * 未授权异常（401）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e) {
        logger.warn("未授权访问: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 禁止访问异常（403）
     */
    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException e) {
        logger.warn("禁止访问: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验失败（@Valid/@Validated）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        logger.warn("参数校验失败: {}", msg);
        return Result.fail(ResponseCodeEnum.PARAM_ERROR.getCode(), msg);
    }

    /**
     * 请求参数缺失
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        logger.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(ResponseCodeEnum.PARAM_ERROR.getCode(), "缺少参数: " + e.getParameterName());
    }

    /**
     * 参数类型转换错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        logger.warn("参数类型错误: {}", e.getName());
        return Result.fail(ResponseCodeEnum.PARAM_ERROR.getCode(), "参数" + e.getName() + "类型不合法");
    }

    /**
     * 请求体不可读（如JSON格式错误）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.warn("请求体不可读: {}", e.getMessage());
        return Result.fail(ResponseCodeEnum.PARAM_ERROR.getCode(), "请求数据格式错误");
    }

    /**
     * 请求方法不支持（405）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        logger.warn("请求方法不支持: {}", e.getMessage());
        return Result.fail(405, "请求方法不支持，请使用 " + String.join(", ", e.getSupportedMethods()));
    }

    /**
     * 请求路径不存在（404）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        logger.warn("请求路径不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return Result.fail(ResponseCodeEnum.NOT_FOUND);
    }

    /**
     * IllegalArgumentException（参数非法）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数: {}", e.getMessage());
        return Result.fail(ResponseCodeEnum.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 数据库访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        logger.error("数据库异常 [{}] {}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }

    /**
     * SQL异常
     */
    @ExceptionHandler(SQLException.class)
    public Result<Void> handleSQLException(SQLException e) {
        logger.error("SQL异常: {}", e.getMessage(), e);
        return Result.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }

    /**
     * 空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<Void> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        logger.error("空指针异常 [{}] {}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }

    /**
     * 异步（SSE）请求超时异常
     * SSE 响应已以 text/event-stream 提交，不能再返回 JSON 错误体，
     * 否则消息转换器不兼容会引发二次异常。只记录日志即可。
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        logger.warn("SSE 异步请求超时: {}", e.getMessage());
    }

    /**
     * 兜底：未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        logger.error("系统异常 [{}] {}", request.getRequestURI(), e.getMessage(), e);
        return Result.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }
}