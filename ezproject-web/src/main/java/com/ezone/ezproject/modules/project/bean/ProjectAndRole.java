package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.enums.ProjectMemberRole;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Data
public class ProjectAndRole extends Project {
    @ApiModelProperty(value = "扩展信息")
    private Map<String, Object> extend;
    @ApiModelProperty(value = "项目成员角色")
    private ProjectMemberRole role;
    @ApiModelProperty(value = "关联的项目集")
    private List<Portfolio> portfolios;

    public ProjectAndRole(Project project, Map<String, Object> extend, List<Portfolio> portfolios) {
        if (null != project) {
            try {
                BeanUtils.copyProperties(this, project);
            } catch (Exception e) {
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        this.extend = extend;
        this.portfolios = portfolios;
    }

    public ProjectAndRole(Project project, Map<String, Object> extend, ProjectMemberRole role, List<Portfolio> portfolios) {
        this(project, extend, portfolios);
        this.role = role;
    }
}
