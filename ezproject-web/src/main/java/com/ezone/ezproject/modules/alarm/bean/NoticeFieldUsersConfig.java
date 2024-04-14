package com.ezone.ezproject.modules.alarm.bean;

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
public class NoticeFieldUsersConfig implements NoticeUserConfig {
    @NotNull
    private List<String> userFields;
}
