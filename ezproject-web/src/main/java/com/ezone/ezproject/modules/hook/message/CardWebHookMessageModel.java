package com.ezone.ezproject.modules.hook.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.WebHookEventType;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.common.EndpointHelper;
import groovy.json.StringEscapeUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Slf4j
public class CardWebHookMessageModel implements WebHookMessageModel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    private Map<String, Object> cardDetail;
    private WebHookEventType eventType;
    private Project project;
    private CompanyCardSchema companyCardSchema;
    private ProjectCardSchema projectCardSchema;
    private Map<String, String> cardUserNicknames;
    private String user;
    private String userNickOrName;
    private String company;

    private EndpointHelper endpointHelper;

    @Getter(lazy = true)
    private final String title = String.format("%s%s-%d", eventType.getDescription(), project.getKey(), FieldUtil.getSeqNum(cardDetail));

    @Getter(lazy = true)
    private final String content = render("/vm/webhook/card/content.tpl");

    private String render(String resource) {
        try {
            return VelocityTemplate.render(context(), resource);
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }

    private VelocityContext context() {
        VelocityContext context = new VelocityContext();
        context.put("FieldUtil", FieldUtil.class);
        context.put("StringEscapeUtils",  StringEscapeUtils.class);
        context.put("projectName", project.getName());
        context.put("projectKey", project.getKey());
        context.put("userNickOrName", userNickOrName);
        context.put("cardCreateTime", DATE_TIME_FORMATTER.print(FieldUtil.getCreateTime(cardDetail)));
        context.put("cardDetail", cardDetail);
        context.put("cardUrl", endpointHelper.cardDetailUrl(project.getCompanyId(), project.getKey(), FieldUtil.getSeqNum(cardDetail)));
        context.put("userUrl", endpointHelper.userHomeUrl(project.getCompanyId(), user));
        context.put("cardTitle", FieldUtil.getTitle(cardDetail));
        context.put("cardTypeName", companyCardSchema.findCardTypeName(FieldUtil.getType(cardDetail)));
        context.put("eventType", eventType);
        context.put("ownerUserNickNames", getOwnerUserNicknames(cardDetail, cardUserNicknames));
        CardField cardField = projectCardSchema.findCardField(CardField.PRIORITY);
        context.put("cardPriority", FieldUtil.getValidNameInOptions(FieldUtil.getPriority(cardDetail), cardField));
        return context;
    }

    /**
     * 飞书
     * @return
     */
    @Override
    public String getRichTextContent() {
        return render("/vm/webhook/card/content.richtext.tpl");
    }

    /**
     * 企业微信、叮叮
     * @return
     */
    @Override
    public String getEscapeMdContent() {
        return StringEscapeUtils.escapeJava(render("/vm/webhook/card/content.tpl"));
    }

    public static String getOwnerUserNicknames(Map<String, Object> cardDetail, Map<String, String> cardUserUserNicknameMap){
        List<String> ownerUsers = FieldUtil.getOwnerUsers(cardDetail);
        String ownerUserNickNames;
        if (CollectionUtils.isEmpty(ownerUsers)) {
            ownerUserNickNames = "";
        } else {
            List<String> nickNames = new ArrayList<>();
            ownerUsers.forEach(username -> nickNames.add(cardUserUserNicknameMap.get(username)));
            ownerUserNickNames = StringUtils.join(nickNames, ",");
        }
        return ownerUserNickNames;
    }
}
