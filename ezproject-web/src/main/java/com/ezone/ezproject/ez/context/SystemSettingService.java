package com.ezone.ezproject.ez.context;

import com.ezone.ezbase.iam.bean.SystemFuncSetting;
import com.ezone.ezbase.iam.service.AuthUtil;
import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.configuration.CacheManagers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class SystemSettingService {
    private AuthUtil authUtil;
    private IAMCenterService iamCenterService;

    @Cacheable(
            cacheManager = CacheManagers.TRANSIENT_CACHE_MANAGER,
            cacheNames = "SystemService.bpmIsOpen",
            key = "'bpmIsOpen'"
    )
    public boolean bpmIsOpen() {
        SystemFuncSetting systemFuncSetting = iamCenterService.querySystemFuncSetting();
        return systemFuncSetting.isEzBPM();
    }

}
