package com.ezone.ezproject.common.auth;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAuthType {
    TokenAuthType value() default TokenAuthType.READ;
}
