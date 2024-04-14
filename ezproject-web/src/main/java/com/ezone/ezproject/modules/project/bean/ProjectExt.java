package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Project;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Data
public class ProjectExt extends Project {
    @ApiModelProperty(value = "扩展信息")
    private Map<String, Object> extend;
    private boolean favourite;

    public ProjectExt(Project project, Map<String, Object> extend) {
        if (null != project) {
            try {
                BeanUtils.copyProperties(this, project);
            } catch (Exception e) {
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        this.extend = extend;
    }

    public ProjectExt(Project project, Map<String, Object> extend, boolean favourite) {
        this(project, extend);
        this.favourite = favourite;
    }

    public ProjectExt(Long id, Map<String, Object> extend) {
        this.setId(id);
        this.extend = extend;
    }

    public void set(Project project, boolean favourite) {
        if (null != project) {
            try {
                BeanUtils.copyProperties(this, project);
            } catch (Exception e) {
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        this.favourite = favourite;
    }
}
