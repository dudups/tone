package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.dal.entity.PortfolioMember;
import com.ezone.ezproject.dal.entity.ProjectMember;
import com.ezone.ezproject.es.entity.enums.RoleSource;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MemberBean {
    @ApiModelProperty(value = "用户类型")
    @NotNull
    private GroupUserType userType;

    @ApiModelProperty(value = "成员用户名")
    @NotEmpty
    private String user;

    @ApiModelProperty(value = "成员角色")
    @NotNull
    private String role;

    @ApiModelProperty(value = "成员角色")
    @NotNull
    private RoleSource source;

    public static MemberBean from(ProjectMember member) {
        return MemberBean.builder()
                .userType(GroupUserType.valueOf(member.getUserType()))
                .user(member.getUser())
                .role(member.getRole())
                .source(RoleSource.valueOf(member.getRoleSource()))
                .build();
    }

    public static MemberBean from(PortfolioMember member) {
        return MemberBean.builder()
                .userType(GroupUserType.valueOf(member.getUserType()))
                .user(member.getUser())
                .role(member.getRole())
                .source(RoleSource.valueOf(member.getRoleSource()))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MemberBean member = (MemberBean) o;
        return userType == member.userType &&
                source == member.source &&
                Objects.equals(user, member.user) &&
                StringUtils.equals(role, member.role);
    }

    public boolean equals(ProjectMember member) {
        if (member == null) return false;
        return userType == GroupUserType.valueOf(member.getUserType()) &&
                source == RoleSource.valueOf(member.getRoleSource()) &&
                Objects.equals(user, member.getUser()) &&
                StringUtils.equals(role, member.getRole());
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }
}
