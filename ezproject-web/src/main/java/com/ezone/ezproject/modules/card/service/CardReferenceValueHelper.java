package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.alarm.service.AlarmConfigQueryService;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.ReferenceValues;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.ezproject.modules.storymap.service.StoryMapQueryService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardReferenceValueHelper {
    private StoryMapQueryService storyMapQueryService;

    private CardQueryService cardQueryService;

    private PlanQueryService planQueryService;

    private ProjectQueryService projectQueryService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private AlarmConfigQueryService alarmConfigQueryService;

    public static final List<String> REF_FIELDS = Arrays.asList(CardField.PLAN_ID, CardField.PARENT_ID, CardField.STORY_MAP_NODE_ID, CardField.PROJECT_ID);

    public void refIds(@NonNull ReferenceValues ref, @NonNull String field, List<Long> ids) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        switch (field) {
            case CardField.PLAN_ID:
                ref.setPlans(planQueryService.select(ids).stream().collect(Collectors.toMap(Plan::getId, p -> p)));
                break;
            case CardField.PARENT_ID:
                ref.setParents(
                        cardQueryService.selectDetail(ids, CardField.TITLE, CardField.SEQ_NUM).entrySet().stream().collect(Collectors.toMap(
                                e -> e.getKey(), e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())));
                break;
            case CardField.STORY_MAP_NODE_ID:
                ref.setStoryMapNodes(storyMapQueryService.selectStoryMapNodeByIds(ids).stream().collect(Collectors.toMap(StoryMapNode::getId, s -> s)));
                break;
            case CardField.PROJECT_ID:
                ref.setProjects(projectQueryService.select(ids).stream().collect(Collectors.toMap(Project::getId, p -> p)));
                break;
            default:
        }
    }

    public void refCards(@NonNull ReferenceValues ref, @NonNull final String field, List<Map<String, Object>> cards) throws IOException {
        if (REF_FIELDS.contains(field)) {
            refIds(ref, field, ids(cards, field));
        }
    }

    public void refValues(@NonNull ReferenceValues ref, @NonNull final String field, List<?> values) throws IOException {
        if (REF_FIELDS.contains(field)) {
            refIds(ref, field, ids(values));
        }
    }

    /**
     * 前提：确保先加载了ref.projects
     */
    public void tryLoadProjectSchemas(ReferenceValues ref, String... fields) {
        tryLoadProjectSchemas(ref, fields == null ? ListUtils.EMPTY_LIST : Arrays.asList(fields));
    }

    /**
     * 前提：确保先加载了ref.projects
     */
    public void tryLoadFullProjectSchemas(ReferenceValues ref, String... fields) {
        List<String> fieldList = Arrays.asList(fields);
        if (ref == null || MapUtils.isEmpty(ref.getProjects())) {
            return;
        }
        if (!(fieldList.contains(CardField.TYPE) || fieldList.contains(CardField.STATUS))) {
            return;
        }

        Map<Long, ProjectCardSchema> schemas = new HashMap<>();
        List<Long> projectIds = new ArrayList<>(ref.getProjects().keySet());
        for (Long projectId : projectIds) {
            ProjectCardSchema projectCardSchema = projectSchemaQueryService.getProjectCardSchema(projectId);
            schemas.put(projectId, projectCardSchema);
        }
        ref.setProjectSchemas(schemas);
    }

    /**
     * 前提：确保先加载了ref.projects
     */
    private void tryLoadProjectSchemas(ReferenceValues ref, List<String> fields) {
        if (ref == null || MapUtils.isEmpty(ref.getProjects())) {
            return;
        }
        if (!(fields.contains(CardField.TYPE) || fields.contains(CardField.STATUS))) {
            return;
        }
        List<Long> projectIds = new ArrayList<>(ref.getProjects().keySet());

        Map<Long, ProjectCardSchema> schemas = projectSchemaQueryService.find(projectIds, ArrayUtils.add(ProjectCardSchema.TYPES_STATUSES_FIELDS_FOR_UI, "fields"));
        if (CollectionUtils.isNotEmpty(fields)) {
            for (Map.Entry<Long, ProjectCardSchema> schemaEntry : schemas.entrySet()) {
                ProjectCardSchema schema = schemaEntry.getValue();
                if (CollectionUtils.isNotEmpty(schema.getFields())) {
                    schema.setFields(schema.getFields().stream().filter(field -> fields.contains(field.getKey())).collect(Collectors.toList()));
                }
            }
        }
        ref.setProjectSchemas(schemas);
    }

    /**
     * 前提：确保先加载了ref.projects
     */
    public void tryLoadProjectAlarm(ReferenceValues ref) {
        if (ref == null || MapUtils.isEmpty(ref.getProjects())) {
            return;
        }
        List<Long> projectIds = new ArrayList<>(ref.getProjects().keySet());
        List<ProjectAlarmExt> projectAlarms = alarmConfigQueryService.getProjectAlarms(projectIds, true);
        ref.setAlarms(projectAlarms);
    }

    private List<Long> ids(List<Map<String, Object>> cards, String field) {
        if (CollectionUtils.isEmpty(cards)) {
            return ListUtils.EMPTY_LIST;
        }
        return cards.stream().map(card -> FieldUtil.toLong(card.get(field))).filter(v -> null != v && v > 0).collect(Collectors.toList());
    }

    private List<Long> ids(List<?> values) {
        return values.stream().map(FieldUtil::toLong).filter(v -> null != v && v > 0).collect(Collectors.toList());
    }

}
