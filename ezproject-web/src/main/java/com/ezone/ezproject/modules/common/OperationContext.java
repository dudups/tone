package com.ezone.ezproject.modules.common;

import com.ezone.ezbase.iam.util.RequestFilterUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Slf4j
@Getter
public class OperationContext {
    private final String userName;
    private final String ip;
    private final Date time;
    private final long currentTimeMillis;

    private OperationContext(String userName, String ip) {
        this.userName = userName;
        this.ip = ip;
        this.currentTimeMillis = System.currentTimeMillis();
        this.time = new Date(this.currentTimeMillis);
    }

    public static OperationContext instance(String user, String ip) {
        return new OperationContext(user, ip);
    }

    public static OperationContext instance(String user) {
        return new OperationContext(user, ip());
    }

    private static String ip() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                return RequestFilterUtil.getRemoteIpAddr(((ServletRequestAttributes) requestAttributes).getRequest());
            }
        } catch (Exception e) {
            log.error("[ip][error][" + e.getMessage() + "]", e);
        }
        return StringUtils.EMPTY;
    }
}
