package com.ezone.ezproject.common.exception;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

@Getter
public class CodedException extends HttpStatusCodeException {
    public static final int ERROR_BASE_CODE = 5000;

    public static final CodedException UNAUTHORIZED = new CodedException(HttpStatus.UNAUTHORIZED, "未认证！");
    public static final CodedException FORBIDDEN = new CodedException(HttpStatus.FORBIDDEN, "无权限！");
    public static final CodedException NOT_FOUND = new CodedException(HttpStatus.NOT_FOUND, "未找到！");
    public static final CodedException DELETED = new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片已删除到回收站，需要先还原！");
    public static final CodedException BAD_REQUEST = new CodedException(HttpStatus.BAD_REQUEST, "错误请求！");
    public static final CodedException INVALID_TOKEN_EXCEPTION = new CodedException(ErrorCode.INVALID_TOKEN, "Token非法或过期！");
    public static final CodedException COMPANY_OUT_FORBIDDEN = new CodedException(HttpStatus.FORBIDDEN, "企业越权!");
    public static final CodedException ERROR_CARD_TYPE = new CodedException(HttpStatus.NOT_ACCEPTABLE, "非法卡片类型!");

    private int code = 0;
    private String message = StringUtils.EMPTY;
    private Object data;

    public CodedException(HttpStatus statusCode) {
        this(statusCode, statusCode.getReasonPhrase());
    }

    public CodedException(HttpStatus statusCode, String message) {
        super(statusCode, message);
        if (HttpStatus.FORBIDDEN == statusCode) {
            this.code = HttpStatus.FORBIDDEN.value();
        } else {
            this.code = ERROR_BASE_CODE + statusCode.value();
        }
        this.message = message;
    }

    public CodedException(int code) {
        this(code, StringUtils.EMPTY);
    }


    public CodedException(int code, String message) {
        super(HttpStatus.OK);
        this.code = code;
        this.message = message;
    }

    public CodedException(int code, String message, Object data) {
        this(code, message);
        this.data = data;
    }
}
