package com.ezone.ezproject.modules.common;

import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.configuration.CacheManagers;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class EndpointHelper {
    private IAMCenterService iamCenterService;

    @Cacheable(
            cacheManager = CacheManagers.TRANSIENT_CACHE_MANAGER,
            cacheNames = "EndpointHelper.companyUrl",
            key = "'name:'.concat(#company)"
    )
    public String companyUrl(String company) {
        return iamCenterService.queryHomeAddressByCompanyName(company);
    }

    @Cacheable(
            cacheManager = CacheManagers.TRANSIENT_CACHE_MANAGER,
            cacheNames = "EndpointHelper.companyUrl",
            key = "'id:'.concat(#company)"
    )
    public String companyUrl(Long company) {
        return iamCenterService.queryHomeAddressByCompanyId(company);
    }

    public String cardDetailUrl(String company, String projectKey, Long cardSeqNum) {
        return companyUrl(company) + cardDetailPath(projectKey, cardSeqNum);
    }

    /**
     * without endpoint domain
     */
    public String cardDetailPath(String projectKey, Long seqNum) {
        return String.format("/project/%s/%d", projectKey, seqNum);
    }

    public String cardDetailUrl(Long company, String projectKey, Long cardSeqNum) {
        return companyUrl(company) + cardDetailPath(projectKey, cardSeqNum);
    }

    public String planDetailPath(String projectKey, Long planId) {
        return String.format("/project/%s/plan/list/%d", projectKey, planId);
    }

    public String planDetailUrl(Long company, String projectKey, Long planId) {
        return companyUrl(company) + planDetailPath(projectKey, planId);
    }

    public String userHomePath(String user) {
        return String.format("/home/%s", user);
    }

    public String userHomeUrl(Long company, String user) {
        return companyUrl(company) + userHomePath(user);
    }
}
