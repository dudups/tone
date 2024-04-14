package com.ezone.ezproject.modules.permission;

import com.ezone.ezbase.iam.bean.Resource;
import com.ezone.ezbase.iam.bean.enums.ResourceType;
import com.ezone.ezbase.iam.service.RBACService;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@AllArgsConstructor
public class CompanyPermissionService {
    private UserService userService;

    private CompanyService companyService;

    private RBACService rbacService;

    public void checkCreateProject() throws CodedException {
        Long companyId = companyService.currentCompany();
        String user = userService.currentUserName();
        Resource resource = new Resource(ResourceType.COMPANY_RESOURCE, companyId);
        if (!rbacService.queryUserHasResourcePermission(companyId, Arrays.asList(resource), user, "PROJECT_ADD")) {
            throw CodedException.FORBIDDEN;
        }
    }

    public void checkCreatePortfolio() throws CodedException {
        Long companyId = companyService.currentCompany();
        String user = userService.currentUserName();
        Resource resource = new Resource(ResourceType.COMPANY_RESOURCE, companyId);
        if (!rbacService.queryUserHasResourcePermission(companyId, Arrays.asList(resource), user, "PORTFOLIO_ADD")) {
            throw CodedException.FORBIDDEN;
        }
    }

}
