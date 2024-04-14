package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.common.EndpointHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class CardMessageHelper {
    private EndpointHelper endpointHelper;

    private UserService userService;

    private CardQueryService cardQueryService;

    public String statusBpmFlowContent(LoginUser user, Project project, Card card, String cardTitle, String fromStatusName, String toStatusName) {
        return String.format(
                "%s在项目[%s]发起卡片%s的状态变更：从[%s]变为[%s]",
                userPathHtml(card.getCompanyId(), user), project.getName(), cardPathHtml(card, cardTitle), fromStatusName, toStatusName);
    }

    public String changeCardTypeAndStatusBpmFlowContent(LoginUser user, Project project, Map<String, Object> cardDetails, String fromStatusName, String toStatusName) {
        return String.format(
                "%s在项目[%s]转换卡片%s的状态变更：从[%s]变为[%s]",
                userPathHtml(project.getCompanyId(), user), project.getName(), cardPathHtml(project, cardDetails), fromStatusName, toStatusName);
    }

    private String cardUrlHtml(Card card) {
        String title = "";
        try {
            title = cardQueryService.selectTitle(card.getId());
        } catch (IOException e) {
            log.error("cardQueryService selectTitle exception!", e);
        }
        return cardUrlHtml(card, title);
    }

    private String cardUrlHtml(Card card, String title) {
        return CardHelper.cardUrlHtml(card, endpointHelper, title);
    }

    private String cardUrlHtml(Project project, Map<String, Object> cardDetails) {
        return CardHelper.cardUrlHtml(project, cardDetails, endpointHelper);
    }

    private String cardPathHtml(Card card, String title) {
        return CardHelper.cardPathHtml(card, endpointHelper, title);
    }

    private String cardPathHtml(Project project, Map<String, Object> cardDetails) {
        return CardHelper.cardPathHtml(project, cardDetails, endpointHelper);
    }

    private String userUrlHtml(Long companyId, LoginUser user) {
        return String.format("<a href='%s'>%s</a>",
                endpointHelper.userHomeUrl(companyId, user.getUsername()),
                userService.userNickOrName(companyId, user));
    }

    protected String userPathHtml(Long companyId, LoginUser user) {
        return String.format("<User>%s</User>", user.getUsername());
    }
}
