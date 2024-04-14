package com.ezone.ezproject.modules.alarm.bean;

import com.ezone.ezbase.iam.bean.GroupUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoticeSpecUsersConfig implements NoticeUserConfig {
    @NotNull
    private List<GroupUser> users;
}
