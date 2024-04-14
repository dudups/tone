package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.card.service.CardHelper;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@Getter
@Slf4j
/**
 * 批量创建、更新
 */
public class CardsOperationMessageModel extends BaseMessageModel {
    private static final int cardUrlsLimitSize = 2;
    private Map<Long, Map<String, Object>> cardDetails;
    private ProjectNoticeConfig.Type operationType;

    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/cardsOperationTypeContent.tpl");
    }

    @Override
    public String getEscapeTitle() {
        return null;
    }

    @Override
    public String getContent() {
        int size = Math.min(cardDetails.size(), cardUrlsLimitSize);
        List<String> cardsUrl = new ArrayList<>();
        List<Map<String, Object>> cardDetailList = new ArrayList<>(cardDetails.values());
        for (int i = 0; i < size; i++) {
            Map<String, Object> cardDetail = cardDetailList.get(i);
            cardsUrl.add(CardHelper.cardPathHtml(project, cardDetail, endpointHelper));
        }
        return String.format("<User>%s</User>批量%s了卡片%s...", sender, operationType.getCnName(), StringUtils.join(cardsUrl, ' '));
    }

    @Override
    public String getEmailContent() {
        int size = Math.min(cardDetails.size(), cardUrlsLimitSize);
        List<String> cardsUrl = new ArrayList<>();
        List<Map<String, Object>> cardDetailList = new ArrayList<>(cardDetails.values());
        for (int i = 0; i < size; i++) {
            Map<String, Object> cardDetail = cardDetailList.get(i);
            cardsUrl.add(CardHelper.cardUrlHtml(project, cardDetail, endpointHelper));
        }
        return String.format("%s批量%s了卡片%s...", userUrlHtml(), operationType.getCnName(), StringUtils.join(cardsUrl, ' '));
    }

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
        context.put("StringEscapeUtils", StringEscapeUtils.class);
        context.put("userNickOrName", nickName);
        context.put("cardDetails", new ArrayList<>(cardDetails.values()));
        context.put("companyId", project.getCompanyId());
        context.put("projectKey", project.getKey());
        context.put("userUrl", userUrl());
        context.put("operationType", operationType);
        context.put("context", this);
        int size = Math.min(cardDetails.size(), cardUrlsLimitSize);
        context.put("size", size);
        return context;
    }

    private String userUrlHtml() {
        return String.format("<a href='%s'>%s</a>", userUrl(), nickName);
    }

    private String userUrl() {
        return endpointHelper.userHomeUrl(project.getCompanyId(), sender);
    }

    public String cardUrl(Long companyId, String projectKey, Long cardSeqNum) {
        return endpointHelper.cardDetailUrl(companyId, projectKey, cardSeqNum);
    }
}
