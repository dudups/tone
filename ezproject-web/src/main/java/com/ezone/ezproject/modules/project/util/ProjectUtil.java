package com.ezone.ezproject.modules.project.util;

import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectField;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectUtil {
    public static Map<String, Object> projectIndexMap(Project project, Map<String, Object> extend) {
        Map<String, Object> map = new HashMap<>();
        if (MapUtils.isNotEmpty(extend)) {
            map.putAll(extend);
        }
        map.put(ProjectField.KEY, project.getKey());
        map.put(ProjectField.NAME, project.getName());
        map.put(ProjectField.COMPANY_ID, project.getCompanyId());
        map.put(ProjectField.DESCRIPTION, project.getDescription());
        map.put(ProjectField.CREATE_TIME, project.getCreateTime().getTime());
        map.put(ProjectField.CREATE_USER, project.getCreateUser());
        map.put(ProjectField.LAST_MODIFY_TIME, project.getLastModifyTime().getTime());
        map.put(ProjectField.LAST_MODIFY_USER, project.getLastModifyUser());
        map.put(ProjectField.TOP_SCORE, project.getTopScore());
        map.put(ProjectField.START_TIME, project.getStartTime());
        map.put(ProjectField.END_TIME, project.getEndTime());
        map.put(ProjectField.IS_ACTIVE, project.getIsActive());
        return map;
    }
}
