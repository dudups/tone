package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.limit.incr.AbstractDailyIncrLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProjectCardDailyLimiter extends AbstractDailyIncrLimiter<Long> {
    public static final String DOMAIN_RESOURCE_KEY = "daily-incr:project:card";

    @Value("${daily-incr-limit.project.card:-1}")
    private Long projectCardLimit;

    @Override
    public Long incrLimit() {
        return projectCardLimit;
    }

    @Override
    public String limitMsg() {
        return "Too many card for project today!";
    }

    @Override
    public String domainResourceKey() {
        return DOMAIN_RESOURCE_KEY;
    }
}
