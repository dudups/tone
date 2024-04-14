package com.ezone.ezproject.ez.context;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezbase.iam.bean.CompanyIdentityUser;
import com.ezone.ezbase.iam.bean.CompanyUser;
import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezbase.iam.bean.enums.UserIdentityStatus;
import com.ezone.ezbase.iam.bean.enums.UserIdentityType;
import com.ezone.ezbase.iam.service.AuthUtil;
import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.common.exception.CodedException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@AllArgsConstructor
public class UserService {
    private AuthUtil authUtil;

    private IAMCenterService iamCenterService;

    private CompanyService companyService;

    public String currentUserName() {
        return authUtil.getUsername();
    }

    public Long currentUserId() {
        return authUtil.getUserId();
    }

    public LoginUser currentUser() {
        return authUtil.getLoginUser();
    }

    public BaseUser user(String userName) {
        return iamCenterService.queryUserByUsername(userName);
    }

    public BaseUser user(Long userId) {
        return iamCenterService.queryUserByUserId(userId);
    }

    public List<BaseUser> queryUsersByUsernames(List<String> userNames) {
        return iamCenterService.queryUsersByUsernames(userNames);
    }

    public boolean isCompanyAdmin(String userName, String companyName) {
        CompanyIdentityUser companyIdentityUser = iamCenterService.queryUserByCompanyNameAndUsername(
                companyName, userName);
        return companyIdentityUser != null
                && UserIdentityStatus.NORMAL.equals(companyIdentityUser.getIdentityStatus())
                && UserIdentityType.ADMIN.equals(companyIdentityUser.getType());
    }

    public boolean isCompanyAdmin(String userName, Long companyId) {
        CompanyIdentityUser companyIdentityUser = iamCenterService.queryUserByCompanyIdAndUsername(
                companyId, userName);
        return companyIdentityUser != null
                && UserIdentityStatus.NORMAL.equals(companyIdentityUser.getIdentityStatus())
                && UserIdentityType.ADMIN.equals(companyIdentityUser.getType());
    }

    public boolean isInCompany(String userName, String companyName) {
        CompanyIdentityUser companyIdentityUser = iamCenterService.queryUserByCompanyNameAndUsername(
                companyName, userName);
        return companyIdentityUser != null
                && UserIdentityStatus.NORMAL.equals(companyIdentityUser.getIdentityStatus());
    }

    public boolean isInCompany(String userName, Long companyId) {
        CompanyIdentityUser companyIdentityUser = iamCenterService.queryUserByCompanyIdAndUsername(
                companyId, userName);
        return companyIdentityUser != null
                && UserIdentityStatus.NORMAL.equals(companyIdentityUser.getIdentityStatus());
    }

    public List<String> queryGroupNamesByCompanyNameAndUsername(String companyName, String userName) {
        return iamCenterService.queryGroupNamesByCompanyNameAndUsername(companyName, userName);
    }

    public List<String> queryGroupNamesByCompanyUsername(Long companyId, String userName) {
        return iamCenterService.queryGroupNamesByCompanyUsername(companyId, userName);
    }

    public boolean validateUserPassword(String user, String password) {
        return iamCenterService.validateUserPassword(user, password);
    }

    public void checkPermission(TokenAuthType target) {
        if (!hasPermission(target)) {
            throw CodedException.FORBIDDEN;
        }
    }

    public boolean hasPermission(TokenAuthType target) {
        CompanyUser companyUser = authUtil.getCompanyUser();
        return TokenAuthType.hasPermission(companyUser.getSessionAuthType(), target);
    }

    public String userNickOrName(Long companyId, LoginUser user) {
        return companyService.enableNickname(companyId) && StringUtils.isNotEmpty(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    public String userNickOrName(Long companyId, BaseUser user) {
        return companyService.enableNickname(companyId) && StringUtils.isNotEmpty(user.getNickname()) ? user.getNickname() : user.getUsername();
    }

    public Map<String, Set<String>> getGroupMember(long companyId, Set<String> groupNames) {
        Map<String, Set<String>> groupMembers = iamCenterService.batchQueryGroupUsername(companyId, groupNames);
        if (groupMembers == null) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "调用ezbase返回成员为null");
        }
        return groupMembers;
    }
}
