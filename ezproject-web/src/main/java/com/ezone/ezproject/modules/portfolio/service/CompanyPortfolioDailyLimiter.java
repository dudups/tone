package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.limit.incr.AbstractDailyIncrLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CompanyPortfolioDailyLimiter extends AbstractDailyIncrLimiter<Long> {
    public static final String DOMAIN_RESOURCE_KEY = "daily-incr:company:portfolio";

    @Value("${daily-incr-limit.company.portfolio:-1}")
    private Long companyPortfolioLimit;

    @Override
    public Long incrLimit() {
        return companyPortfolioLimit;
    }

    @Override
    public String limitMsg() {
        return "Too many portfolio for company today!";
    }

    @Override
    public String domainResourceKey() {
        return DOMAIN_RESOURCE_KEY;
    }
}
