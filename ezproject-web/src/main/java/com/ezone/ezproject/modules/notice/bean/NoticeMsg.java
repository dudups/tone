package com.ezone.ezproject.modules.notice.bean;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezbase.iam.bean.enums.SystemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoticeMsg {
    private long companyId;
    @Builder.Default
    private SystemType systemType = SystemType.EZPROJECT;
    private String sender;
    private String content;
    private NoticeType notificationType;

    // 分人发，无法对直接接收人和邮件组包含接收人去重
    @Deprecated
    private String receiver;
    @Deprecated
    private GroupUserType receiverType;

    private Set<GroupUser> receivers;

    // 是否过滤发送者
    private boolean filterSender;
}
