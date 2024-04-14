package com.ezone.ezproject.modules.alarm.bean;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@ApiModel(value = "角色选择", description = "用于项目与计划预警中的设置")
public class NoticeRoleUserConfig implements NoticeUserConfig {
    @NotNull
    private List<NoticeRole> roles;
}
