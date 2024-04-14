package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.common.EndpointHelper;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
public class BaseMessageModel implements MessageModel {
    Project project;
    String sender;
    String nickName;
    EndpointHelper endpointHelper;

    @Override
    public String getEscapeTitle() {
        return null;
    }

    @Override
    public String getContent() {
        return null;
    }

    @Override
    public String getEmailContent() {
        return MessageModel.super.getEmailContent();
    }

    @Override
    public String getFeiShuContent() {
        return MessageModel.super.getFeiShuContent();
    }

    public static String getOwnerUserNicknames(Map<String, Object> cardDetail, Map<String, String> cardUserUserNicknameMap) {
        List<String> ownerUsers = FieldUtil.getOwnerUsers(cardDetail);
        String ownerUserNickNames;
        if (CollectionUtils.isEmpty(ownerUsers)) {
            ownerUserNickNames = "";
        } else {
            List<String> nickNames = new ArrayList<>();
            ownerUsers.forEach(username -> nickNames.add(cardUserUserNicknameMap.get(username)));
            ownerUserNickNames = StringUtils.join(nickNames, "，");
        }
        return ownerUserNickNames;
    }
}
