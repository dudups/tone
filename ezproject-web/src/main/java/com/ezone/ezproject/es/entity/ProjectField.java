package com.ezone.ezproject.es.entity;

import com.ezone.ezproject.es.entity.enums.FieldType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProjectField {
    @ApiModelProperty(value = "字段标识key", example = "title")
    private String key;
    @ApiModelProperty(value = "字段名", example = "标题")
    @NotNull
    @Size(min = 1, max = 32)
    private String name;
    @ApiModelProperty(value = "字段描述")
    private String description;
    @ApiModelProperty(value = "字段类型")
    private FieldType type;
    @ApiModelProperty(value = "字段值的类型")
    private FieldType.ValueType valueType;
    @ApiModelProperty(value = "针对下拉、复选等字段类型的选项列表")
    @Singular
    private List<Option> options;

    @ApiModelProperty(value = "字段是否启用")
    private boolean enable;
    @ApiModelProperty(value = "是否必填")
    private boolean required;

    public static final String KEY = "key";
    public static final String NAME = "name";
    public static final String COMPANY_ID = "companyId";
    public static final String DESCRIPTION = "description";
    public static final String CREATE_TIME = "createTime";
    public static final String CREATE_USER = "createUser";
    public static final String LAST_MODIFY_TIME = "lastModifyTime";
    public static final String LAST_MODIFY_USER = "lastModifyUser";
    public static final String TOP_SCORE = "topScore";
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    public static final String IS_ACTIVE = "isActive";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Option {
        @ApiModelProperty(value = "选项名")
        private String name;
        @ApiModelProperty(value = "选项具体描述")
        private String description;
    }
}
