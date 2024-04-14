package com.ezone.ezproject.common.limit.incr;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CompanyIncrLimit {
    String domainResourceKey();

    /**
     * Spring Expression Language (SpEL) expression for computing the incr dynamically
     */
    String incr() default "1";
}
