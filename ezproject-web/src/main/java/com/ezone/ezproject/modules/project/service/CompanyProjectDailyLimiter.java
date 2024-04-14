package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.limit.incr.AbstractDailyIncrLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CompanyProjectDailyLimiter extends AbstractDailyIncrLimiter<Long> {
    public static final String DOMAIN_RESOURCE_KEY = "daily-incr:company:project";

    @Value("${daily-incr-limit.company.project:-1}")
    private Long companyProjectLimit;

    @Override
    public Long incrLimit() {
        return companyProjectLimit;
    }

    @Override
    public String limitMsg() {
        return "Too many project for company today!";
    }

    @Override
    public String domainResourceKey() {
        return DOMAIN_RESOURCE_KEY;
    }
}
