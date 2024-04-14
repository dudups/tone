package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.ProjectWikiSpace;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.service.ProjectWikiSpaceService;
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

@ApiOperation("项目关联wiki空间")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/wikiSpace")
@Slf4j
@AllArgsConstructor
public class ProjectWikiSpaceController extends AbstractController {
    private ProjectWikiSpaceService projectWikiSpaceService;

    @ApiOperation("添加关联wiki空间")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ProjectWikiSpace> bind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                               @ApiParam(value = "wiki空间ID") @NotNull @RequestParam Long spaceId) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        return success(projectWikiSpaceService.bind(projectId, spaceId));
    }

    @ApiOperation("获取关联wiki空间")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<RelatesBean<ProjectWikiSpace>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) {
        checkHasProjectRead(projectId);
        return success(projectWikiSpaceService.selectRelatesBean(projectId));
    }

    @ApiOperation("移除关联wiki空间")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId, @ApiParam(value = "关联ID", example = "1") @PathVariable Long id) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectWikiSpaceService.delete(id);
        return SUCCESS_RESPONSE;
    }
}
