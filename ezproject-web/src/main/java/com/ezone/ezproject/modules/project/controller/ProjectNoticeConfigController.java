package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.bean.ProjectConfigRequest;
import com.ezone.ezproject.modules.project.service.ProjectNoticeConfigService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;
import java.io.IOException;

@ApiOperation("项目通知配置")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/notice")
@Slf4j
@AllArgsConstructor
public class ProjectNoticeConfigController extends AbstractController {
    private ProjectNoticeConfigService projectNoticeConfigService;

    @ApiOperation("保存项目通知配置")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse save(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                             @ApiParam(value = "配置内容") @Size(min = 1) @RequestBody ProjectConfigRequest request)
            throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectNoticeConfigService.saveOrUpdate(projectId, request);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询项目通知配置")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectNoticeConfig> selectByPlanId(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId)
            throws IOException {
        checkHasProjectRead(projectId);
        return success(projectNoticeConfigService.getProjectNoticeConfig(projectId));
    }
}
