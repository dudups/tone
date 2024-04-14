package com.ezone.ezproject.modules.hook.controller;

import com.ezone.ezbase.iam.service.IAMCenterService;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.WebHookProject;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.es.entity.enums.WebHookEventType;
import com.ezone.ezproject.modules.hook.service.WebHookProjectCmdService;
import com.ezone.ezproject.modules.hook.service.WebHookProjectQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@ApiOperation("WebHook关联项目事件")
@RestController
@RequestMapping("/project/webhook/{webHookId:[0-9]+}/project")
@Slf4j
@AllArgsConstructor
public class WebHookProjectController extends AbstractController {
    private WebHookProjectQueryService webHookProjectQueryService;
    private WebHookProjectCmdService webHookProjectCmdService;
    private IAMCenterService iamCenterService;

    @ApiOperation("add or update")
    @PutMapping("{projectId:[0-9]+}")
    public BaseResponse<WebHookProject> saveOrUpdate(@ApiParam(value = "Hook ID", example = "1") @PathVariable Long webHookId,
                                                     @ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId,
                                                     @ApiParam(value = "event列表") @RequestBody List<WebHookEventType> eventTypes)
            throws IOException {
        checkHasHookAdmin(webHookId);
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        return success(webHookProjectCmdService.saveOrUpdate(webHookId, projectId, eventTypes));
    }

    @ApiOperation("delete")
    @DeleteMapping("{projectId:[0-9]+}")
    public BaseResponse delete(@ApiParam(value = "Hook ID", example = "1") @PathVariable Long webHookId,
                               @ApiParam(value = "项目ID", example = "1") @PathVariable Long projectId)
            throws IOException {
        checkHasHookAdmin(webHookId);
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        webHookProjectCmdService.delete(webHookId, projectId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询")
    @GetMapping
    public BaseResponse<TotalBean<WebHookProject>> select(@ApiParam(value = "Hook ID", example = "1") @PathVariable Long webHookId)
            throws IOException {
        return success(webHookProjectQueryService.findByWebHookId(webHookId));
    }

    private void checkHasHookAdmin(Long hookId) {
        if (!iamCenterService.isHookAdmin(companyService.currentCompany(), hookId, userService.currentUserId())) {
            throw CodedException.FORBIDDEN;
        }
    }
}
