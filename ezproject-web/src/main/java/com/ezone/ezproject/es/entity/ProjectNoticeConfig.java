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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/***
 * 项目中消息通知配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectNoticeConfig {

    @ApiModelProperty(value = "最近修改时间")
    private Date lastModifyTime;

    @ApiModelProperty(value = "最近修改人")
    private String lastModifyUser;

    private Config planNoticeConfig;

    private Config cardNoticeConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {

        @ApiModelProperty(value = "是不开启该类型的通知")
        private boolean isOpen;

        @ApiModelProperty(value = "通知范围")
        private List<Type> notifyTypes;

        @ApiModelProperty(value = "通知用户来源 USER_LIST,PROJECT_MEMBERS")
        private UsersType usersType;

        @ApiModelProperty(value = "通知用户")
        private List<User> users;
    }

    public enum Type {
        /**
         * 创建
         */
        CREATE("创建"),
        /**
         * 删除
         */
        DELETE("删除"),
        /**
         * 更新
         */
        UPDATE("更新"),
        /**
         * 还原
         */
        REVERT("还原"),
        /**
         * 催办
         */
        REMIND("催办");

        private String cnName;

        Type(String cnName) {
            this.cnName = cnName;
        }

        public String getCnName() {
            return cnName;
        }
    }

    public enum UsersType {
        /**
         * 角色管理中所有角色中除了访客
         */
        PROJECT_MEMBERS,

        /**
         * 指定用户列表
         */
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

    public static ProjectNoticeConfig DEFAULT_CONFIG = ProjectNoticeConfig.builder().planNoticeConfig(
            ProjectNoticeConfig.Config.builder()
                    .isOpen(true)
                    .notifyTypes(Arrays.asList(ProjectNoticeConfig.Type.CREATE, ProjectNoticeConfig.Type.DELETE, ProjectNoticeConfig.Type.UPDATE))
                    .usersType(ProjectNoticeConfig.UsersType.PROJECT_MEMBERS)
                    .build())
            .cardNoticeConfig(ProjectNoticeConfig.Config.builder()
                    .isOpen(true)
                    .notifyTypes(Arrays.asList(ProjectNoticeConfig.Type.values()))
                    .build())
            .build();
    ;
}
