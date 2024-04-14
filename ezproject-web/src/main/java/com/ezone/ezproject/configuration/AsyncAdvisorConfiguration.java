package com.ezone.ezproject.configuration;

import com.ezone.ezproject.Application;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync(order = Ordered.LOWEST_PRECEDENCE - 1)
public class AsyncAdvisorConfiguration {
    @Bean
    public BeanPostProcessor asyncAdvisorSetBeforeExistingAdvisor() {
        return new BeanPostProcessor() {
            private boolean first = true;
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (first) {
                    first = false;
                    Application.context().getBean(AsyncAnnotationBeanPostProcessor.class).setBeforeExistingAdvisors(false);
                }
                return bean;
            }
        };
    }
}
