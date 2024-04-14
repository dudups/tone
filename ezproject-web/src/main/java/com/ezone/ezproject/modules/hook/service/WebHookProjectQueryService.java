package com.ezone.ezproject.modules.hook.service;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.dao.WebHookProjectDao;
import com.ezone.ezproject.es.entity.WebHookProject;
import com.ezone.ezproject.es.entity.enums.WebHookEventType;
import com.ezone.ezproject.modules.card.bean.ReferenceValues;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class WebHookProjectQueryService {
    private WebHookProjectDao webHookProjectDao;
    
    private ProjectQueryService projectQueryService;

    public TotalBean<WebHookProject> findByWebHookId(Long webHookId) throws IOException {
        List<WebHookProject> hooks = webHookProjectDao.search(Eq.builder().field(WebHookProject.WEB_HOOK_ID).value(String.valueOf(webHookId)).build());
        Map<Long, Project> projects = MapUtils.EMPTY_MAP;
        if (CollectionUtils.isNotEmpty(hooks)) {
            projects = projectQueryService.select(hooks.stream().map(h -> h.getProjectId()).collect(Collectors.toList()))
                    .stream()
                    .collect(Collectors.toMap(Project::getId, p -> p));
        }
        return TotalBean.<WebHookProject>builder()
                .total(hooks.size())
                .list(hooks)
                .refs(ReferenceValues.builder().projects(projects).build())
                .build();
    }

    public WebHookProject find(Long id) throws IOException {
        return webHookProjectDao.find(id);
    }

    public Long findId(Long webHookId, Long projectId) throws IOException {
        List<Long> ids = webHookProjectDao.searchIds(
                Eq.builder().field(WebHookProject.WEB_HOOK_ID).value(String.valueOf(webHookId)).build(),
                Eq.builder().field(WebHookProject.PROJECT_ID).value(String.valueOf(projectId)).build()
        );
        return CollectionUtils.isEmpty(ids) ? null : ids.get(0);
    }

    public List<Long> findWebHookIds(Long projectId, WebHookEventType eventType) {
        return findWebHookIds(projectId, Arrays.asList(eventType));
    }

    public List<Long> findWebHookIds(Long projectId, List<WebHookEventType> eventTypes) {
        try {
            List<WebHookProject> hooks = webHookProjectDao.search(
                    Arrays.asList(
                            Eq.builder().field(WebHookProject.PROJECT_ID).value(String.valueOf(projectId)).build(),
                            In.builder().field(WebHookProject.EVENT_TYPES).values(eventTypes.stream().map(WebHookEventType::name).collect(Collectors.toList())).build()),
                    WebHookProject.WEB_HOOK_ID);
            if (CollectionUtils.isEmpty(hooks)) {
                return ListUtils.EMPTY_LIST;
            }
            return hooks.stream().map(WebHookProject::getWebHookId).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("findWebHookIds IOException!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
