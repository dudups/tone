package com.ezone.ezproject.modules.project.controller;

import com.ezone.devops.ezcode.base.enums.ResourceType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.ProjectRepoBean;
import com.ezone.ezproject.modules.project.service.ProjectRepoService;
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
import java.util.List;

@ApiOperation("项目成员操作")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/repo")
@Slf4j
@AllArgsConstructor
public class ProjectRepoController extends AbstractController {
    private ProjectRepoService projectRepoService;

    @ApiOperation("添加关联代码库")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<List<ProjectRepoBean>> create(@ApiParam(value = "项目ID", example = "1", required = true) @PathVariable Long projectId,
                                                      @ApiParam(value = "代码库路径或父目录", required = true) @NotNull @RequestParam Long resourceId,
                                                      @ApiParam(value = "资源类型(此处可选：DIRECTORY, REPO)", required = true) @NotNull @RequestParam ResourceType resourceType,
                                                      @ApiParam(value = "是否包含所有子目录下的代码库") @NotNull @RequestParam(required = false, defaultValue = "false") boolean recursion
    ) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        return success(projectRepoService.bindWithCheck(projectId, resourceId, resourceType, recursion));
    }

    @ApiOperation("获取关联代码库")
    @GetMapping
    public BaseResponse<List<ProjectRepoBean>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) {
        checkHasProjectRead(projectId);
        return success(projectRepoService.selectBeanByProjectId(projectId));
    }

    @ApiOperation("移除关联代码库")
    @DeleteMapping("{id:[0-9]+}")
    public BaseResponse delete(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                               @ApiParam(value = "关联ID", example = "1") @PathVariable Long id) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectRepoService.delete(id);
        return SUCCESS_RESPONSE;
    }
}
