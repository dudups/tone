package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.ProjectK8sGroup;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.service.ProjectK8sGroupService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@ApiOperation("项目关联k8s集群")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/k8sGroup")
@Slf4j
@AllArgsConstructor
public class ProjectK8sGroupController extends AbstractController {
    private ProjectK8sGroupService projectK8sGroupService;

    @ApiOperation("添加关联k8s集群")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ProjectK8sGroup> bind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                              @ApiParam(value = "wiki空间ID") @NotNull @RequestParam Long k8sGroupId) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        return success(projectK8sGroupService.bind(projectId, k8sGroupId));
    }

    @ApiOperation("获取关联k8s集群")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<RelatesBean<ProjectK8sGroup>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) {
        checkHasProjectRead(projectId);
        return success(projectK8sGroupService.selectRelatesBean(projectId));
    }

    @ApiOperation("移除关连k8s集群")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId, @ApiParam(value = "关联ID", example = "1") @PathVariable Long id) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectK8sGroupService.delete(id);
        return SUCCESS_RESPONSE;
    }
}
