package com.ezone.ezproject.ez.context;

import com.ezone.ezbase.iam.bean.CompanyUser;
import com.ezone.ezbase.iam.bean.enums.UserIdentityType;
import com.ezone.ezbase.iam.bean.setting.MemberSetting;
import com.ezone.ezbase.iam.service.AuthUtil;
import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class CompanyService {
    private AuthUtil authUtil;
    private IAMCenterService iamCenterService;

    public Long currentCompany() {
        CompanyUser companyUser = authUtil.getCompanyUser();
        if (UserIdentityType.GUEST.equals(companyUser.getUserIdentityType())) {
            throw CodedException.COMPANY_OUT_FORBIDDEN;
        }
        return companyUser.getCompanyId();
    }

    public String currentCompanyName() {
        CompanyUser companyUser = authUtil.getCompanyUser();
        if (UserIdentityType.GUEST.equals(companyUser.getUserIdentityType())) {
            throw CodedException.COMPANY_OUT_FORBIDDEN;
        }
        return companyUser.getCompanyName();
    }

    public String companyName(Long companyId) {
        return iamCenterService.getCompanyNameByCompanyId(companyId);
    }

    @Cacheable(
            cacheManager = CacheManagers.TRANSIENT_CACHE_MANAGER,
            cacheNames = "CompanyService.enableNickname",
            key = "'id:'.concat(#companyId)"
    )
    public boolean enableNickname(Long companyId) {
        MemberSetting setting = iamCenterService.getMemberSettingByCompanyId(companyId);
        return setting != null && setting.isEnableNickname();
    }

}
