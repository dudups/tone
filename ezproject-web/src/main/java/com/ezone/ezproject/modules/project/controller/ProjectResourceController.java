package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.ProjectHostGroup;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import com.ezone.ezproject.modules.project.service.ProjectResourceService;
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

@ApiOperation("项目关联计算资源")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/resource")
@Slf4j
@AllArgsConstructor
public class ProjectResourceController extends AbstractController {
    private ProjectResourceService projectResourceService;

    @ApiOperation("添加关联主机组")
    @PostMapping("hostGroup")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectHostGroup> bind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                                 @ApiParam(value = "主机组ID") @NotNull @RequestParam Long groupId) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        return success(projectResourceService.bind(userService.currentUserName(), projectId, groupId, true));
    }

    @ApiOperation("获取关联主机组")
    @GetMapping("hostGroup")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<RelatesBean<ProjectHostGroup>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId) {
        checkHasProjectRead(projectId);
        return success(projectResourceService.selectRelatesBean(projectId));
    }

    @ApiOperation("移除关联主机组")
    @DeleteMapping("hostGroup/{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unbind(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                               @ApiParam(value = "关联ID", example = "1") @PathVariable Long id) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectResourceService.delete(id);
        return SUCCESS_RESPONSE;
    }
}
