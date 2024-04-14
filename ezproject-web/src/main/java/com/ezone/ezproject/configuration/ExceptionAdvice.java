package com.ezone.ezproject.configuration;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.modules.card.excel.CustomerExcelAnalysisException;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.HttpCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

@RestControllerAdvice("com.ezone.ezproject")
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity bindException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder error = new StringBuilder();
        error.append("请求参数校验未通过!");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            error.append("\n").append(fieldError.getField()).append(": ").append(fieldError.getDefaultMessage());
        }
        return ResponseEntity.ok(new BaseResponse(HttpCode.BAD_REQUEST).withMessage(error.toString()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity bindException(ConstraintViolationException e) {
        StringBuilder error=new StringBuilder();
        error.append("请求参数校验未通过!");
        for (ConstraintViolation violation : e.getConstraintViolations()) {
            error.append("\n").append(violation.getPropertyPath()).append(": ").append(violation.getMessage());
        }
        return ResponseEntity.ok(new BaseResponse(HttpCode.BAD_REQUEST).withMessage(error.toString()));
    }

    @ExceptionHandler({CodedException.class})
    public ResponseEntity handleCodedException(CodedException e) {
        return ResponseEntity.ok(new BaseResponse(e.getCode(), e.getData(), e.getMessage()));
    }
    @ExceptionHandler({CustomerExcelAnalysisException.class})
    public ResponseEntity handleCodedException(CustomerExcelAnalysisException e) {
        return ResponseEntity.ok(new BaseResponse(e.getCode(), null, e.getMessage()));
    }

    @ExceptionHandler({HttpStatusCodeException.class})
    public ResponseEntity handleHttpStatusCodeException(HttpStatusCodeException e) {
        if (HttpStatus.FORBIDDEN == e.getStatusCode()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getStatusText());
        }
        return ResponseEntity.ok(new BaseResponse(CodedException.ERROR_BASE_CODE + e.getStatusCode().value(),
                null, e.getStatusText()));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity handleException(HttpServletRequest request, Throwable e) {
        log.error(String.format("%s url:[%s] exception!", request.getMethod(), request.getRequestURI()), e);
        return ResponseEntity.ok(new BaseResponse(CodedException.ERROR_BASE_CODE + HttpStatus.INTERNAL_SERVER_ERROR.value(),
                null, e.getMessage()));
    }
}
