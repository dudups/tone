package com.ezone.ezproject.es.entity;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 2022.6.23废弃，计划与卡片通知统一由项目通知配置进行管理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Deprecated
public class PlanNotice {
    @ApiModelProperty(value = "是否开启")
    private boolean enable;

    @ApiModelProperty(value = "通知用户来源")
    @NotNull
    private UsersType usersType;

    @ApiModelProperty(value = "通知指定用户列表")
    private List<User> users;

    public enum UsersType {
        /**角色管理中所有角色中除了访客*/
        PROJECT_MEMBERS,

        /**指定用户列表*/
        USER_LIST
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class User {
        @ApiModelProperty(value = "用户类型")
        @NotNull
        private GroupUserType userType;

        @ApiModelProperty(value = "成员用户名")
        @NotEmpty
        private String user;

        public GroupUser toGroupUser() {
            return new GroupUser(user, userType);
        }
    }
}
