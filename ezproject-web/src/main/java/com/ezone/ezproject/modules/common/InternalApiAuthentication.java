package com.ezone.ezproject.modules.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InternalApiAuthentication {
    String HEADER_TIMESTAMP = "X-INTERNAL-AUTH-TIMESTAMP";
    String HEADER_MD5 = "X-INTERNAL-AUTH-MD5";

    /**
     * token信息位置
     * @return
     */
    Location location() default Location.HTTP_HEADER;

    /**
     * 时间戳（值单位MS）属性名/参数名
     * @return
     */
    String timestamp() default InternalApiAuthentication.HEADER_TIMESTAMP;

    /**
     * MD5属性名/参数名
     * @return
     */
    String md5() default InternalApiAuthentication.HEADER_MD5;

    /**
     * 过期时间MS
     * @return
     */
    long expireMs() default 5000;

    enum Location {
        HTTP_HEADER, METHOD_PARAM
    }
}
