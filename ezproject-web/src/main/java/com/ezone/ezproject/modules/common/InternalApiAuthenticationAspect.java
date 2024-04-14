package com.ezone.ezproject.modules.common;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Objects;

@Order(1)
@Aspect
//@Component
@Slf4j
public class InternalApiAuthenticationAspect {
    @Value("${internal.api.token}")
    private String token;

    @Pointcut("within(com.ezone.ezproject.modules.*.controller.*ApiController)")
    public void controller() { }

    @Pointcut("@annotation(com.ezone.ezproject.modules.common.InternalApiAuthentication)")
    public void auth() {}

    @Around("controller() && auth() && @annotation(internalApiAuthentication)")
    public Object around(ProceedingJoinPoint pjp, InternalApiAuthentication internalApiAuthentication) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        switch (internalApiAuthentication.location()) {
            case HTTP_HEADER:
                HttpServletRequest request = attributes.getRequest();
                checkAuth(request.getHeader(internalApiAuthentication.md5()),
                        NumberUtils.toLong(request.getHeader(internalApiAuthentication.timestamp()), 0L),
                        internalApiAuthentication.expireMs());
                break;
            case METHOD_PARAM:
                checkAuth((String) getArg(pjp, internalApiAuthentication.md5()),
                        (Long) getArg(pjp, internalApiAuthentication.timestamp()),
                        internalApiAuthentication.expireMs());
                break;
            default:
                throw CodedException.INVALID_TOKEN_EXCEPTION;
        }
        return pjp.proceed();
    }

    private void checkAuth(String md5, Long timestamp, long expireMs) {
        if (StringUtils.isEmpty(md5) || null == timestamp) {
            throw CodedException.INVALID_TOKEN_EXCEPTION;
        }
        if (System.currentTimeMillis() - timestamp > expireMs) {
            throw CodedException.INVALID_TOKEN_EXCEPTION;
        }
        if (!DigestUtils.md5Hex(this.token + timestamp).equals(md5)) {
            throw CodedException.INVALID_TOKEN_EXCEPTION;
        }
    }

    private Object getArg(JoinPoint jp, String argName) {
        Object valueParameter = null;
        if (Objects.nonNull(jp) && jp.getSignature() instanceof MethodSignature
                && Objects.nonNull(argName) ) {
            MethodSignature method = (MethodSignature)jp.getSignature();
            String[] parameters = method.getParameterNames();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].equals(argName)) {
                    Object[] obj = jp.getArgs();
                    valueParameter = obj[i];
                }
            }
        }
        return valueParameter;
    }
}
