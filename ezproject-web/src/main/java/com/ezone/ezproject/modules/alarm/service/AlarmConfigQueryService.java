package com.ezone.ezproject.modules.alarm.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.mapper.ExtProjectAlarmMapper;
import com.ezone.ezproject.es.dao.ProjectAlarmDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.NoticeFieldUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.PlanAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class AlarmConfigQueryService {
    private ProjectAlarmDao projectAlarmDao;
    private ProjectSchemaQueryService schemaQueryService;
    private ExtProjectAlarmMapper projectAlarmMapper;

    public @NotNull List<ProjectAlarmExt> getProjectAlarms(Long projectId) {
        List<ProjectAlarmExt> items;
        try {
            items = projectAlarmDao.findByProjectId(projectId);
        } catch (IOException e) {
            log.error("[getProjectAlarm][" + " projectId :" + projectId + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        return items;
    }

    public @NotNull List<ProjectAlarmExt> getProjectAlarms(Long projectId, Boolean active) {
        List<ProjectAlarmExt> items;
        try {
            items = projectAlarmDao.findByProjectId(projectId, active);
        } catch (IOException e) {
            log.error("[getProjectAlarm][" + " projectId :" + projectId + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        return items;
    }

    public @NotNull List<ProjectAlarmExt> getProjectAlarms(List<Long> projectIds) {
        List<ProjectAlarmExt> items;
        try {
            items = projectAlarmDao.findByProjectIds(projectIds, null);
        } catch (IOException e) {
            log.error("[getProjectAlarm][" + " projectId :" + projectIds + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        return items;
    }

    public @NotNull List<ProjectAlarmExt> getProjectAlarms(List<Long> projectIds, Boolean active) {
        List<ProjectAlarmExt> items;
        try {
            items = projectAlarmDao.findByProjectIds(projectIds, active);
        } catch (IOException e) {
            log.error("[getProjectAlarm][" + " projectId :" + projectIds + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        return items;
    }

    public @NotNull List<ProjectAlarmExt> getProjectAlarms(Boolean active, List<AlarmItem.Type> types, Integer pageNumber, Integer pageSize) {
        List<ProjectAlarmExt> items;
        try {
            items = projectAlarmDao.search(active, types, pageNumber, pageSize, ProjectAlarmExt.FIELD_ES_PROJECT_ID, SortOrder.DESC);
        } catch (IOException e) {
            log.error("[getProjectAlarms][" + " pageNumber :" + pageNumber + "; pageSize :" + pageSize + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (items == null) {
            items = Collections.emptyList();
        }
        return items;
    }

    public @NotNull List<Long> haveAlarmConfigProjectIds() {
        return projectAlarmMapper.selectAllProjectIds();
    }

    public void validAlarmItem(Long projectId, AlarmItem alarmItem) {
        String dateFieldKey = alarmItem.getDateFieldKey();
        if (alarmItem instanceof CardAlarmItem) {
            ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
            Set<String> allKeys = projectCardSchema.getFields().stream().map(CardField::getKey).collect(Collectors.toSet());
            if (!allKeys.contains(dateFieldKey)) {
                throw new CodedException(HttpStatus.NOT_FOUND, String.format("预警时间字段[%s]未找到", dateFieldKey));
            }
            alarmItem.getWarningUsers().forEach(noticeUserConfig -> {
                if (noticeUserConfig instanceof NoticeFieldUsersConfig) {
                    NoticeFieldUsersConfig fieldUsersConfig = (NoticeFieldUsersConfig) noticeUserConfig;
                    fieldUsersConfig.getUserFields().forEach(userFieldKey -> {
                        if (!allKeys.contains(userFieldKey)) {
                            throw new CodedException(HttpStatus.NOT_FOUND, String.format("用户字段[%s]未找到", userFieldKey));
                        }
                    });
                }
            });
        } else if (alarmItem instanceof ProjectAlarmItem) {
            if (!(ProjectAlarmItem.FIELD_KEY_START_TIME.equals(dateFieldKey) || ProjectAlarmItem.FIELD_KEY_END_TIME.equals(dateFieldKey))) {
                throw new CodedException(HttpStatus.NOT_FOUND, String.format("预警时间字段[%s]未找到", dateFieldKey));
            }
        } else if (alarmItem instanceof PlanAlarmItem) {
            if (!(PlanAlarmItem.FIELD_KEY_START_TIME.equals(dateFieldKey) || PlanAlarmItem.FIELD_KEY_END_TIME.equals(dateFieldKey))) {
                throw new CodedException(HttpStatus.BAD_REQUEST, String.format("预警时间字段[%s]未找到", dateFieldKey));
            }
        }
    }
}
