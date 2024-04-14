package com.ezone.ezproject.common.limit.incr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Order(1)
@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class IncrLimitAspect {
    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();

    private List<IncrLimiter> incrLimiters;

    @Getter(lazy = true)
    private final Map<String, IncrLimiter> limiterMap = CollectionUtils.isEmpty(incrLimiters) ? MapUtils.EMPTY_MAP :
            incrLimiters.stream().collect(Collectors.toMap(IncrLimiter::domainResourceKey, l -> l));

    @Getter(lazy = true)
    private final Map<String, Class> limiterDomainClassMap = CollectionUtils.isEmpty(incrLimiters) ? MapUtils.EMPTY_MAP :
            incrLimiters.stream().collect(Collectors.toMap(IncrLimiter::domainResourceKey, l -> GenericTypeResolver.resolveTypeArgument(l.getClass(), IncrLimiter.class)));

    @Pointcut("@annotation(com.ezone.ezproject.common.limit.incr.IncrLimit)")
    public void limit() {}

    @Around("limit() && @annotation(incrLimit)")
    public Object around(ProceedingJoinPoint pjp, IncrLimit incrLimit) throws Throwable {
        String domainResourceKey = incrLimit.domainResourceKey();
        IncrLimiter incrLimiter = getLimiterMap().get(domainResourceKey);
        if (incrLimiter != null) {
            EvaluationContext evaluationContext = evaluationContext(pjp);
            Class domainClass = getLimiterDomainClassMap().get(domainResourceKey);
            incrLimiter.check(
                    SPEL_PARSER.parseExpression(incrLimit.domainId()).getValue(evaluationContext, domainClass),
                    SPEL_PARSER.parseExpression(incrLimit.incr()).getValue(evaluationContext, Long.class));
        }
        return pjp.proceed();
    }

    private EvaluationContext evaluationContext(JoinPoint jp) {
        MethodSignature method = (MethodSignature)jp.getSignature();
        return new MethodBasedEvaluationContext(jp.getTarget(), method.getMethod(), jp.getArgs(), new DefaultParameterNameDiscoverer());
    }

}
