package com.ezone.ezproject.modules.hook.service;

import com.ezone.ezbase.iam.bean.BaseUser;
import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezbase.iam.bean.WebHookMsg;
import com.ezone.ezbase.iam.bean.enums.HookPlatform;
import com.ezone.ezbase.iam.bean.enums.SystemType;
import com.ezone.ezbase.iam.dal.model.WebHook;
import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.dao.WebHookProjectDao;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.WebHookProject;
import com.ezone.ezproject.es.entity.enums.WebHookEventType;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.hook.message.CardWebHookMessageModel;
import com.ezone.ezproject.modules.hook.message.WebHookMessageModel;
import com.ezone.ezproject.modules.notice.NoticeService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.galaxy.framework.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class WebHookProjectCmdService {
    private WebHookProjectDao webHookProjectDao;

    private WebHookProjectQueryService webHookProjectQueryService;

    private EndpointHelper endpointHelper;

    private IAMCenterService iamCenterService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private NoticeService noticeService;

    private UserService userService;

    public WebHookProject saveOrUpdate(Long webHookId, Long projectId, List<WebHookEventType> eventTypes) throws IOException {
        Long id = webHookProjectQueryService.findId(webHookId, projectId);
        if (id == null) {
            id = IdUtil.generateId();
        }
        WebHookProject hook = WebHookProject.builder()
                .webHookId(webHookId)
                .projectId(projectId)
                .eventTypes(eventTypes)
                .build();
        webHookProjectDao.saveOrUpdate(id, hook);
        return hook;
    }

    public void delete(Long id) throws IOException {
        webHookProjectDao.delete(id);
    }

    public void delete(Long webHookId, Long projectId) throws IOException {
        webHookProjectDao.delete(
                Eq.builder().field(WebHookProject.WEB_HOOK_ID).value(String.valueOf(webHookId)).build(),
                Eq.builder().field(WebHookProject.PROJECT_ID).value(String.valueOf(projectId)).build()
        );
    }

    public void deleteByProjectId(Long projectId) throws IOException {
        webHookProjectDao.delete(Eq.builder().field(WebHookProject.PROJECT_ID).value(String.valueOf(projectId)).build());
    }

    public void deleteByWebHookId(Long webHookId) throws IOException {
        webHookProjectDao.delete(Eq.builder().field(WebHookProject.WEB_HOOK_ID).value(String.valueOf(webHookId)).build());
    }

    @Async
    public void asyncSendCardWebHookMsg(Map<String, Object> cardDetail, Project project, WebHookEventType eventType, String company, LoginUser user) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(project.getId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
        asyncSendWebHookMsg(CardWebHookMessageModel.builder()
                .companyCardSchema(companyCardSchema)
                .eventType(eventType)
                .cardDetail(cardDetail)
                .cardUserNicknames(cardUserNicknames)
                .projectCardSchema(schema)
                .project(project)
                .user(user.getUsername())
                .userNickOrName(userService.userNickOrName(project.getCompanyId(), user))
                .company(company)
                .endpointHelper(endpointHelper)
                .build());
    }


    @Async
    public void asyncSendCardWebHookMsg(Map<String, Object> cardDetail, Project project, WebHookEventType eventType, String company, BaseUser user) {
        CompanyCardSchema companyCardSchema = companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId());
        ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(project.getId());
        Map<String, String> cardUserNicknames = noticeService.getCardUserNicknames(schema, cardDetail);
        asyncSendWebHookMsg(CardWebHookMessageModel.builder()
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(schema)
                .eventType(eventType)
                .cardDetail(cardDetail)
                .cardUserNicknames(cardUserNicknames)
                .project(project)
                .user(user.getUsername())
                .userNickOrName(userService.userNickOrName(project.getCompanyId(), user))
                .company(company)
                .endpointHelper(endpointHelper)
                .build());
    }

    @Async
    public void asyncSendCardsWebHookMsg(Map<Long, Map<String, Object>> cards, Project project, WebHookEventType eventType, String company, LoginUser user) throws IOException {
        cards.forEach((id, cardDetail)  -> asyncSendCardWebHookMsg(cardDetail, project, eventType, company, user));
    }

    @Async
    public void asyncSendCardsWebHookMsg(Map<Long, Map<String, Object>> cards, Project project,  WebHookEventType eventType, String company, BaseUser user) throws IOException {
        cards.forEach((id, cardDetail) -> asyncSendCardWebHookMsg(cardDetail, project, eventType, company, user));
    }

    @Async
    public void asyncSendWebHookMsg(CardWebHookMessageModel model) {
        List<Long> webHookIds = webHookProjectQueryService.findWebHookIds(model.getProject().getId(), model.getEventType());
        if (CollectionUtils.isEmpty(webHookIds)) {
            return;
        }
        List<WebHook> hooks = iamCenterService.queryHooksByIds(webHookIds);
        if (CollectionUtils.isEmpty(hooks)) {
            return;
        }
        long now = System.currentTimeMillis();
        hooks.stream().filter(WebHook::isActive).forEach(hook -> {
            WebHookMsg msg = new WebHookMsg();
            msg.setWebHookId(hook.getId());
            msg.setType(SystemType.EZPROJECT);
            msg.setEventName(model.getEventType().name());
            msg.setResourceName(model.getProject().getKey());
            msg.setTimestamp(now);
            if (HookPlatform.CUSTOM.equals(hook.getPlatform())) {
                msg.setRequestBody(JsonUtils.toJson(model.getCardDetail()));
            } else {
                msg.setRequestBody(requestBodyForMsgPlatform(hook.getPlatform(), model));
            }
            iamCenterService.sendWebHook(msg);
        });
    }

    @Async
    public void asyncSendWebHookMsg(List<Long> webHookIds, WebHookMsg webHookMsg) {
        webHookIds.forEach(webHookId -> {
            webHookMsg.setWebHookId(webHookId);
            iamCenterService.sendWebHook(webHookMsg);
        });
    }

    @Async
    public void asyncSendWebHookMsg(Long projectId, WebHookEventType eventType, WebHookMsg webHookMsg) {
        List<Long> webHookIds = webHookProjectQueryService.findWebHookIds(projectId, eventType);
        if (CollectionUtils.isEmpty(webHookIds)) {
            return;
        }
        webHookIds.forEach(webHookId -> {
            webHookMsg.setWebHookId(webHookId);
            iamCenterService.sendWebHook(webHookMsg);
        });
    }

    String requestBodyForMsgPlatform(HookPlatform platform, WebHookMessageModel model) {
        try {
            return VelocityTemplate.render("model", model, String.format("/vm/webhook/platform/%s.json", platform));
        } catch (Exception e) {
            log.error("Render msg exception!", e);
            return e.getMessage();
        }
    }

    public void asyncSendCardWebHookMsg(Map<Long, Map<String, Object>> cardDetails, Project project, String companyName, LoginUser user) {
        cardDetails.forEach((cardId, details) -> {
            asyncSendCardWebHookMsg(details, project, WebHookEventType.UPDATE_CARD, companyName, user);
        });
    }
}
