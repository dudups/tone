package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardHelper;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Map;

@SuperBuilder
@Getter
@Slf4j
/**
 * 创建更新卡片
 */
public class CardOperationMessageModel extends BaseMessageModel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private CompanyCardSchema companyCardSchema;
    private ProjectCardSchema projectCardSchema;

    private Map<String, Object> cardDetail;
    private ProjectNoticeConfig.Type operationType;
    //成员字段的用户名及昵你
    private Map<String, String> cardUserNickNames;

    public String getEscapeTitle() {
        return String.format("%s %s了卡片：[%s-%s]", nickName, operationType.getCnName(), project.getKey(), FieldUtil.getSeqNum(cardDetail));
    }

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/operationTypeContent.tpl");
    }

    @Override
    public String getContent() {
        return String.format("%s %s了卡片%s", userPathHtml(), operationType.getCnName(), cardPathHtml());
    }

    @Override
    public String getEmailContent() {
        return render("/vm/notice/card/mail/operationTypeContent.html");
    }

    protected String render(String resource) {
        try {
            return VelocityTemplate.render(context(), resource);
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }

    protected VelocityContext context() {
        VelocityContext context = new VelocityContext();
        context.put("StringEscapeUtils", StringEscapeUtils.class);
        context.put("FieldUtil", FieldUtil.class);
        context.put("projectKey", projectKey());
        context.put("cardDetail", cardDetail);
        context.put("operationType", operationType);
        context.put("userNickOrName", nickName);
        context.put("userUrl", endpointHelper.userHomeUrl(project.getCompanyId(), sender));
        context.put("projectName", project.getName());
        context.put("cardTitle", cardTitle());
        context.put("cardUrl", carUrl());
        context.put("cardTypeName", companyCardSchema.findCardTypeName(FieldUtil.getType(cardDetail)));
        context.put("cardCreateTime", DATE_TIME_FORMATTER.print(FieldUtil.getCreateTime(cardDetail)));
        context.put("ownerUserNickNames", getOwnerUserNicknames(cardDetail, cardUserNickNames));
        CardField cardField = projectCardSchema.findCardField(CardField.PRIORITY);
        context.put("cardPriority", FieldUtil.getValidNameInOptions(FieldUtil.getPriority(cardDetail), cardField));
        return context;
    }

    protected String userUrlHtml() {
        return String.format("<a href='%s'>%s</a>", endpointHelper.userHomeUrl(project.getCompanyId(), sender), nickName);
    }

    protected String userPathHtml() {
        return String.format("<User>%s</User>", sender);
    }


    protected String carUrl() {
        return endpointHelper.cardDetailUrl(project.getCompanyId(), project.getKey(), FieldUtil.getSeqNum(cardDetail));
    }

    protected String cardUrlHtml() {
        return CardHelper.cardUrlHtml(project, cardDetail, endpointHelper);
    }

    protected String cardPathHtml() {
        return CardHelper.cardPathHtml(project, cardDetail, endpointHelper);
    }

    protected String cardTitle() {
        return FieldUtil.getTitle(cardDetail);
    }

    protected String projectKey() {
        return project.getKey();
    }

    protected Long seqNum() {
        return FieldUtil.getSeqNum(cardDetail);
    }

}
