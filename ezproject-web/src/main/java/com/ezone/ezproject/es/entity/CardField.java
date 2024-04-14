package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.Source;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardField {
    @ApiModelProperty(value = "字段ID", example = "id")
    private Long id;
    @ApiModelProperty(value = "字段标识key", example = "title")
    private String key;
    @ApiModelProperty(value = "字段名", example = "标题")
    @NotNull
    @Size(min = 1, max = 32)
    private String name;
    @ApiModelProperty(value = "字段描述")
    private String description;
    @NotNull
    @ApiModelProperty(value = "字段来源")
    private Source source;
    @NotNull
    @ApiModelProperty(value = "字段类型")
    private FieldType type;
    @ApiModelProperty(value = "字段值的类型")
    private FieldType.ValueType valueType;
    @ApiModelProperty(value = "针对来源于系统内置的字段，声明其限制为：内置/必选/可选")
    private SysFieldLimit limit;
    @ApiModelProperty(value = "针对下拉、复选等字段类型的选项列表")
    @Singular
    private List<Option> options;
    @ApiModelProperty(value = "是否自动设置字段值默认值")
    private boolean setDefault;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Option {
        @ApiModelProperty(value = "选项ID", example = "id")
        private String key;
        @ApiModelProperty(value = "选项名")
        private String name;
        @ApiModelProperty(value = "选项具体描述")
        private String description;
    }

    public enum SysFieldLimit {
        BUILD_IN, REQUIRED, OPTIONAL
    }

    public static final String CUSTOM_PREFIX = "custom_";

    public static final String TYPE = "type";
    public static final String INNER_TYPE = "inner_type";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";
    public static final String SEQ_NUM = "seq_num";
    public static final String STATUS = "status";
    public static final String OWNER_USERS = "owner_users";
    public static final String AT_USERS = "at_users";
    public static final String WATCH_USERS = "watch_users";
    public static final String COMPANY_ID = "company_id";
    public static final String PROJECT_ID = "project_id";
    public static final String PLAN_ID = "plan_id";
    public static final String PARENT_ID = "parent_id";
    public static final String RELATED_CARD_IDS = "related_card_ids";
    public static final String STORY_MAP_NODE_ID = "story_map_node_id";
    public static final String DELETED = "deleted";
    public static final String CREATE_USER = "create_user";
    public static final String CREATE_TIME = "create_time";
    public static final String LAST_MODIFY_USER = "last_modify_user";
    public static final String LAST_MODIFY_TIME = "last_modify_time";
    public static final String REPO = "repo";
    public static final String QA_OWNER_USERS = "qa_owner_users";
    public static final String FOLLOW_USERS = "follow_users";
    public static final String EXTERNAL_FOLLOW_USERS = "external_follow_users";

    public static final String ESTIMATE_WORKLOAD = "estimate_workload";
    public static final String ACTUAL_WORKLOAD = "actual_workload";
    public static final String REMAIN_WORKLOAD = "remain_workload";
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";

    public static final String IMPORTANCE = "importance";

    public static final String BLOCKED = "blocked";

    public static final String BPM_FLOW_ID = "bpm_flow_id";
    public static final String BPM_FLOW_TO_STATUS = "bpm_flow_to_status";

    public static final String CALC_IS_END = "calc_is_end";
    public static final String LAST_END_TIME = "last_end_time";
    public static final String LAST_END_DELAY = "last_end_delay";
    public static final String PROJECT_IS_ACTIVE = "project_is_active";
    public static final String PLAN_IS_ACTIVE = "plan_is_active";
    public static final String FIRST_PLAN_ID = "first_plan_id";
    public static final String LAST_STATUS_MODIFY_TIME = "last_status_modify_time";

    public static final String PRIORITY = "priority";

    public static Set<String> FIELD_FLOW_INVALID_UPSTREAM_FIELD_KEYS = setOf(
            // 暂时不支持；TYPE,STATUS：
            // 1. 这俩关键字段本已耦合大量逻辑+批量修改，依赖于先重构+定逻辑细节，否则维护成本高&质量易失控;
            // 2. 就算逻辑角度正确了，作为用户角度也很难理解最终的结果；
            TYPE, STATUS,
            DELETED,
            SEQ_NUM,
            PROJECT_ID,
            COMPANY_ID,
            CREATE_USER,
            CREATE_TIME,
            LAST_MODIFY_USER,
            LAST_MODIFY_TIME,
            RELATED_CARD_IDS,
            WATCH_USERS,
            CALC_IS_END,
            LAST_END_TIME,
            LAST_END_DELAY,
            PLAN_IS_ACTIVE,
            FIRST_PLAN_ID,
            BPM_FLOW_ID,
            BPM_FLOW_TO_STATUS,
            PARENT_ID,
            CONTENT
    );

    public static Set<String> FIELD_FLOW_INVALID_DOWNSTREAM_FIELD_KEYS = setOf(
            FIELD_FLOW_INVALID_UPSTREAM_FIELD_KEYS,
            TYPE,
            STATUS
    );


    /**
     * 干系人
     */
    public static List<String> STAKEHOLDER_FIELD_KEYS = Arrays.asList(
            QA_OWNER_USERS, FOLLOW_USERS, EXTERNAL_FOLLOW_USERS
    );

    public static <T> Set<T> setOf(Collection<T> collection, T... objs) {
        Set<T> set = new HashSet<>();
        if (CollectionUtils.isNotEmpty(collection)) {
            set.addAll(collection);
        }
        if (objs != null && objs.length > 0) {
            for (T o : objs) {
                set.add(o);
            }
        }
        return set;
    }

    public static <T> Set<T> setOf(T... objs) {
        if (objs == null || objs.length == 0) {
            return new HashSet<>();
        }
        return Arrays.stream(objs).collect(Collectors.toSet());
    }
}
