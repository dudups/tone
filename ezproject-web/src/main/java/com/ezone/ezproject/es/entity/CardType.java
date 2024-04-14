package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.common.validate.Uniq;
import com.ezone.ezproject.es.entity.bean.AutoStatusFlowEventFilterConf;
import com.ezone.ezproject.es.entity.enums.InnerCardType;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardType {
    @ApiModelProperty(value = "卡片类型标识key", example = "story")
    private String key;
    @ApiModelProperty(value = "卡片内部类型", example = "Story")
    private InnerCardType innerType;
    @Deprecated
    @ApiModelProperty(value = "卡片类型名称", example = "Story")
    private String name;
    @Deprecated
    @ApiModelProperty(value = "卡片颜色", example = "#FFFFFF")
    private String color;
    @ApiModelProperty(value = "卡片类型来源")
    private Source source;
    @Deprecated
    @ApiModelProperty(value = "卡片类型具体描述")
    private String description;
    @ApiModelProperty(value = "卡片类型是否启用")
    private boolean enable;
    @ApiModelProperty(value = "定义卡片字段列表")
    @Singular
    @Uniq(field = "key", message = "字段key不能重复设置！")
    private List<FieldConf> fields;
    @ApiModelProperty(value = "定义卡片状态列表，有序")
    @Singular
    @Uniq(field = "key", message = "状态key不能重复设置！")
    private List<StatusConf> statuses;
    @ApiModelProperty(value = "定义卡片自动流转配置")
    @Singular
    private List<AutoStatusFlowConf> autoStatusFlows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class StatusConf {
        @ApiModelProperty(value = "卡片状态标识key", example = "open")
        private String key;
        @ApiModelProperty(value = "卡片状态是否视为结束状态")
        @JsonProperty("isEnd")
        private boolean isEnd;
        @ApiModelProperty(value = "卡片状态下，卡片是否为只读（除状态及所属计划外，其他字段是否可以编辑）")
        @JsonProperty("isReadOnly")
        private boolean isReadOnly;
        @ApiModelProperty(value = "状态流转：可流转到的目标状态的详细设置")
        @Singular
        @Uniq(field = "targetStatus", message = "目标状态不能重复设置！")
        private List<StatusFlowConf> statusFlows;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class StatusFlowConf {
        @ApiModelProperty(value = "可流转到的目标状态", example = "closed")
        private String targetStatus;
        @ApiModelProperty(value = "通知开关")
        private boolean notice;
        @ApiModelProperty(value = "可操作成员->用户型字段;置空代表取决于项目成员权限", example = "owner")
        private String opUserField;
        @ApiModelProperty(value = "绑定的bpm审批流模版ID")
        private Long bpmFlowTemplateId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class AutoStatusFlowConf {
        @ApiModelProperty(value = "触发自动状态流转的事件类型")
        private AutoStatusFlowEventType eventType;
        @ApiModelProperty(value = "可流转到的目标状态", example = "closed")
        private String targetStatus;
        @ApiModelProperty(value = "检查流转规则")
        private boolean check;
        @ApiModelProperty(value = "通知开关")
        private boolean notice;
        @ApiModelProperty(value = "事件过滤")
        private AutoStatusFlowEventFilterConf eventFilterConf;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FieldConf {
        @ApiModelProperty(value = "字段标识key", example = "title")
        private String key;
        @ApiModelProperty(value = "字段是否启用")
        private boolean enable;
        @ApiModelProperty(value = "不同状态下字段必填只读等限制，可以为空代表任何状态下对改字段都无限制")
        @Uniq(field = "status", message = "字段的状态限制不能重复设置！")
        private List<FieldStatusLimit> statusLimits;

        public FieldLimit findStatusLimit(String status) {
            if (CollectionUtils.isEmpty(statusLimits)) {
                return null;
            }
            for (FieldStatusLimit limit : statusLimits) {
                if (StringUtils.equals(limit.getStatus(), status)) {
                    return limit.getLimit();
                }
            }
            return null;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FieldStatusLimit {
        @ApiModelProperty(value = "状态标识key", example = "open")
        private String status;
        @ApiModelProperty(value = "字段限制，可为null代表OPTIONAL无限制")
        private FieldLimit limit;
    }

    public enum FieldLimit {
        REQUIRED, SUGGEST, OPTIONAL, READ_ONLY, HIDE
    }

    @NotNull
    public List<String> findRequiredFields(String status) {
        List<String> requiredFields = new ArrayList<>();
        requiredFields.add(CardField.TITLE);
        fields.stream()
                .filter(f -> f.isEnable())
                .filter(f -> {
                    List<FieldStatusLimit> statusLimits = f.getStatusLimits();
                    if (CollectionUtils.isEmpty(statusLimits)) {
                        return false;
                    }
                    return statusLimits.stream()
                            .filter(l -> status.equals(l.getStatus()))
                            .anyMatch(l -> FieldLimit.REQUIRED == l.getLimit());
                })
                .map(f -> f.getKey())
                .forEach(requiredFields::add);
        return requiredFields;
    }

    @NotNull
    public List<String> findReadonlyFields(String status) {
        List<String> readonlyFields = new ArrayList<>();
        fields.stream()
                .filter(f -> f.isEnable())
                .filter(f -> {
                    List<FieldStatusLimit> statusLimits = f.getStatusLimits();
                    if (CollectionUtils.isEmpty(statusLimits)) {
                        return false;
                    }
                    return statusLimits.stream()
                            .filter(l -> status.equals(l.getStatus()))
                            .anyMatch(l -> FieldLimit.READ_ONLY == l.getLimit());
                })
                .map(f -> f.getKey())
                .forEach(readonlyFields::add);
        return readonlyFields;
    }

    public FieldConf findFieldConf(String key) {
        return fields.stream().filter(f -> f.getKey().equals(key)).findAny().orElse(null);
    }

    public StatusConf findStatusConf(String key) {
        return statuses.stream().filter(s -> s.getKey().equals(key)).findAny().orElse(null);
    }

    public List<String> findStatusKeys(boolean isEnd) {
        return statuses.stream().filter(s -> s.isEnd == isEnd).map(s -> s.getKey()).collect(Collectors.toList());
    }

    public AutoStatusFlowConf findAutoStatusFlow(AutoStatusFlowEventType eventType) {
        return autoStatusFlows.stream().filter(f -> f.getEventType().equals(eventType)).findAny().orElse(null);
    }

    public StatusFlowConf findStatusFlow(String from, String to) {
        StatusConf statusConf = findStatusConf(from);
        if (CollectionUtils.isEmpty(statusConf.getStatusFlows())) {
            return null;
        }
        return statusConf.getStatusFlows().stream().filter(f -> f.getTargetStatus().equals(to)).findAny().orElse(null);
    }
}
