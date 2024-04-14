package com.ezone.ezproject.modules.project.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.ProjectNoticeBoard;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.project.service.ProjectNoticeBoardService;
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

@ApiOperation("项目通告板")
@RestController
@RequestMapping("/project/project/{projectId:[0-9]+}/board")
@Slf4j
@AllArgsConstructor
public class ProjectNoticeBoardController extends AbstractController {
    private ProjectNoticeBoardService projectNoticeBoardService;

    @ApiOperation("保存通告板")
    @PutMapping
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse save(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                             @ApiParam(value = "通告板内容") @Size(min = 1) @RequestBody String content)
            throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        projectNoticeBoardService.saveOrUpdate(projectId, content);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询通告板")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<ProjectNoticeBoard> selectByPlanId(@ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId)
            throws IOException {
        checkHasProjectRead(projectId);
        return success(projectNoticeBoardService.find(projectId));
    }
}
