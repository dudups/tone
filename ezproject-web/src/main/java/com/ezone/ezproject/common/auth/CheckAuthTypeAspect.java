package com.ezone.ezproject.common.auth;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.ez.context.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Order(1)
@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class CheckAuthTypeAspect {
    public static final CodedException FORBIDDEN = new CodedException(HttpStatus.FORBIDDEN, "当前Token无权限！");

    private UserService userService;

    @Pointcut("@annotation(com.ezone.ezproject.common.auth.CheckAuthType)")
    public void check() {}

    @Around("check() && @annotation(checkAuthType)")
    public Object around(ProceedingJoinPoint pjp, CheckAuthType checkAuthType) throws Throwable {
        TokenAuthType authType = checkAuthType.value();
        if (!userService.hasPermission(authType)) {
            throw FORBIDDEN;
        }
        return pjp.proceed();
    }

}
