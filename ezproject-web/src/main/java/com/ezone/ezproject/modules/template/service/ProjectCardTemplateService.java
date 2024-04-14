package com.ezone.ezproject.modules.template.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.ProjectCardTemplate;
import com.ezone.ezproject.dal.entity.ProjectCardTemplateExample;
import com.ezone.ezproject.dal.mapper.ProjectCardTemplateMapper;
import com.ezone.ezproject.es.dao.CardTemplateDao;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.ez.context.UserService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectCardTemplateService {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper(new JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private UserService userService;

    private CardTemplateDao cardTemplateDao;

    private ProjectCardTemplateMapper cardTemplateMapper;

    private static final Map<String, Map<String, Object>> SYS_TEMPLATES;
    static {
        try {
            SYS_TEMPLATES =  ProjectCardSchema.YAML_MAPPER.readValue(
                    ProjectCardSchema.class.getResource("/sys-card-template.yaml"),
                    new TypeReference<Map<String, Map<String, Object>>>() {}
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Map<String, Object> getProjectCardTemplate(Long projectId, String cardType) throws IOException {
        ProjectCardTemplate projectCardTemplate = select(projectId, cardType);
        if (null != projectCardTemplate) {
            return cardTemplateDao.findAsMap(projectCardTemplate.getId());
        }
        return SYS_TEMPLATES.get(cardType);
    }

    public Map<String, Map<String, Object>> getProjectCardTemplates(Long projectId) throws IOException {
        ProjectCardTemplateExample example = new ProjectCardTemplateExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        List<ProjectCardTemplate> projectCardTemplates = cardTemplateMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(projectCardTemplates)) {
            return MapUtils.EMPTY_MAP;
        }
        Map<Long, String> idTypeMap = projectCardTemplates.stream().collect(
                Collectors.toMap(t -> t.getId(), t -> t.getCardType()));
        return cardTemplateDao.findAsMap(new ArrayList<>(idTypeMap.keySet()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> idTypeMap.get(e.getKey()), e -> e.getValue()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void setProjectCardTemplate(Long projectId, String cardType, Map<String, Object> template) throws IOException {
        String user = userService.currentUserName();
        ProjectCardTemplate projectCardTemplate = select(projectId, cardType);
        if (null == projectCardTemplate) {
            projectCardTemplate = ProjectCardTemplate.builder()
                    .id(IdUtil.generateId())
                    .projectId(projectId)
                    .cardType(cardType)
                    .createUser(user)
                    .createTime(new Date())
                    .lastModifyUser(user)
                    .lastModifyTime(new Date())
                    .build();
            cardTemplateMapper.insert(projectCardTemplate);
        } else {
            projectCardTemplate.setLastModifyUser(user);
            projectCardTemplate.setLastModifyTime(new Date());
            cardTemplateMapper.updateByPrimaryKey(projectCardTemplate);
        }
        cardTemplateDao.saveOrUpdate(projectCardTemplate.getId(), template);
    }

    @Transactional(rollbackFor = Exception.class)
    public void initProjectCardTemplates(Long projectId, Map<String, Map<String, Object>> cardTemplates) throws IOException {
        if (MapUtils.isEmpty(cardTemplates)) {
            return;
        }
        String user = userService.currentUserName();
        Map<Long, Map<String, Object>> templates = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : cardTemplates.entrySet()) {
            String cardType = entry.getKey();
            Long id = IdUtil.generateId();
            cardTemplateMapper.insert(ProjectCardTemplate.builder()
                    .id(id)
                    .projectId(projectId)
                    .cardType(cardType)
                    .createUser(user)
                    .createTime(new Date())
                    .lastModifyUser(user)
                    .lastModifyTime(new Date())
                    .build());
            templates.put(id, entry.getValue());
        }
        cardTemplateDao.saveOrUpdate(templates);
    }

    private ProjectCardTemplate select(Long projectId, String cardType) {
        ProjectCardTemplateExample example = new ProjectCardTemplateExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andCardTypeEqualTo(cardType);
        List<ProjectCardTemplate> projectCardTemplates = cardTemplateMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(projectCardTemplates)) {
            return null;
        }
        return projectCardTemplates.get(0);
    }

    private List<ProjectCardTemplate> select(Long projectId) {
        ProjectCardTemplateExample example = new ProjectCardTemplateExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return cardTemplateMapper.selectByExample(example);
    }

    public void deleteByProjectId(Long projectId) throws IOException {
        List<ProjectCardTemplate> projectCardTemplates = select(projectId);
        if (CollectionUtils.isEmpty(projectCardTemplates)) {
            return;
        }
        cardTemplateDao.delete(projectCardTemplates.stream().map(t -> t.getId()).collect(Collectors.toList()));
    }
}
