package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.ProjectArtifactRepo;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.service.ProjectArtifactRepoService;
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

@ApiOperation("项目关联制品库")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/artifactRepo")
@Slf4j
@AllArgsConstructor
public class ProjectArtifactRepoController extends AbstractController {
    private ProjectArtifactRepoService projectArtifactRepoService;

    @ApiOperation("添加关联制品库")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ProjectArtifactRepo> bind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                               @ApiParam(value = "制品库ID") @NotNull @RequestParam Long repoId) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        return success(projectArtifactRepoService.bind(userService.currentUserName(), projectId, repoId, true));
    }

    @ApiOperation("获取关联制品库")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<RelatesBean<ProjectArtifactRepo>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) {
        checkPermission(projectId, OperationType.PROJECT_READ);
        return success(projectArtifactRepoService.selectRelatesBean(projectId));
    }

    @ApiOperation("移除关联制品库")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId, @ApiParam(value = "关联ID", example = "1") @PathVariable Long id) {
        checkPermission(projectId, OperationType.PROJECT_READ);
        projectArtifactRepoService.delete(id);
        return SUCCESS_RESPONSE;
    }
}
