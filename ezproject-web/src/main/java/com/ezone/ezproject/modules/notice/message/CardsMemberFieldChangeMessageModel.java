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
import java.util.Collection;
import java.util.List;
import java.util.Map;


@SuperBuilder
@Getter
@Slf4j
/**
 * 批量卡片人员字段通知
 */
public class CardsMemberFieldChangeMessageModel extends BaseMessageModel {
    private static final int cardUrlsLimitSize = 2;
    private Map<Long, Map<String, Object>> cardDetails;
    private Collection<String> fieldNames;//变化的人员字段
    private MemberOperatedType memberOperatedType;
    private ProjectNoticeConfig.Type cardOperatedType;


    @Override
    public String getFeiShuContent() {
        return render("/vm/notice/card/feiShu/cardsMemberFieldChangeContent.tpl");
    }

    @Override
    public String getEscapeTitle() {
        return null;
    }

    @Override
    public String getContent() {
        List<String> cardsPath = new ArrayList<>();
        int size = Math.min(cardUrlsLimitSize, cardDetails.size());
        int count = 0;
        for (Map.Entry<Long, Map<String, Object>> detailEntity : cardDetails.entrySet()) {
            if (count < size) {
                Map<String, Object> cardDetail = detailEntity.getValue();
                String cardPathHtml = CardHelper.cardPathHtml(project, cardDetail, endpointHelper);
                cardsPath.add(cardPathHtml);
            }
            count++;
        }
        if (memberOperatedType.equals(MemberOperatedType.add)) {
            return String.format("<User>%s</User>在批量%s卡片时，有[%s]等[%s]张卡片中添加了你", sender, cardOperatedType.getCnName(), StringUtils.join(cardsPath, " "), cardDetails.size());
        } else {
            return String.format("<User>%s</User>在批量%s卡片时，有[%s]等[%s]张卡片将你移除", sender, cardOperatedType.getCnName(), StringUtils.join(cardsPath, " "), cardDetails.size());
        }
    }

    @Override
    public String getEmailContent() {
        List<String> cardsUrlHtml = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Object>> detailEntity : cardDetails.entrySet()) {
            Map<String, Object> cardDetail = detailEntity.getValue();
            String cardUrlHtml = CardHelper.cardUrlHtml(project, cardDetail, endpointHelper);
            cardsUrlHtml.add(cardUrlHtml);
        }
        if (memberOperatedType.equals(MemberOperatedType.add)) {
            return String.format("%s在批量%s卡片时，有[%s]等[%s]张卡片中添加了你", userUrlHtml(sender), cardOperatedType.getCnName(), StringUtils.join(cardsUrlHtml, " "), cardDetails.size());
        } else {
            return String.format("%s在批量%s卡片时，有[%s]等[%s]张卡片将你移除", userUrlHtml(sender), cardOperatedType.getCnName(), StringUtils.join(cardsUrlHtml, " "), cardDetails.size());
        }
    }

    private String render(String resource) {
        try {
            return VelocityTemplate.render(context(), resource);
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }

    private String userUrlHtml(String user) {
        return String.format("<a href='%s'>%s</a>", endpointHelper.userHomeUrl(project.getCompanyId(), user), nickName);
    }

    private VelocityContext context() {
        VelocityContext context = new VelocityContext();
        context.put("StringEscapeUtils", StringEscapeUtils.class);
        context.put("userNickOrName", nickName);
        context.put("cardDetails", new ArrayList<>(cardDetails.values()));
        context.put("companyId", project.getCompanyId());
        context.put("projectKey", project.getKey());
        context.put("userUrl", endpointHelper.userHomeUrl(project.getCompanyId(), sender));
        context.put("cardOperatedType", cardOperatedType);
        context.put("memberOperatedType", memberOperatedType);
        context.put("endpointHelper", endpointHelper);
        int size = Math.min(cardDetails.size(), cardUrlsLimitSize);
        context.put("size", size);
        return context;
    }

    public enum MemberOperatedType {
        add("添加"), delete("删除");

        private final String cnName;

        MemberOperatedType(String cnName) {
            this.cnName = cnName;
        }

        /**
         * 获取操作名称（tpl中有使用）
         *
         * @return 操作名称
         */
        public String getCnName() {
            return cnName;
        }
    }
}
