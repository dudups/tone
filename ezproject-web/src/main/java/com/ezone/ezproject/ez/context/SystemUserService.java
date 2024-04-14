package com.ezone.ezproject.ez.context;

import com.ezone.ezadmin.security.context.BackstageUserContext;
import com.ezone.ezadmin.service.BackstageUserService;
import com.ezone.ezproject.common.exception.CodedException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class SystemUserService {
    private BackstageUserService backstageUserService;

    public String currentUserName() {
        return BackstageUserContext.getUsername();
    }

    public void checkSystemUser() {
        if (!backstageUserService.curUserCanAccessBackstage()) {
            throw CodedException.FORBIDDEN;
        }
    }
}
