package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.dal.entity.Project;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RelatesProjectsBean<R> {
    @ApiModelProperty(value = "关联关系")
    private List<R> relates;

    private List<Project> projects;
}
