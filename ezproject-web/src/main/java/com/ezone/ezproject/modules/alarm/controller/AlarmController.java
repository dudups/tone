package com.ezone.ezproject.modules.alarm.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.service.AlarmConfigCmdService;
import com.ezone.ezproject.modules.alarm.service.AlarmConfigQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@ApiOperation("项目、计划、卡片预警")
@RestController
@RequestMapping("/project/{projectId:[0-9]+}/alarm")
@Slf4j
@AllArgsConstructor
public class AlarmController extends AbstractController {
    private AlarmConfigCmdService alarmConfigCmdService;
    private AlarmConfigQueryService alarmConfigQueryService;

    @ApiOperation("新增配置项")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<ProjectAlarmExt> create(@PathVariable Long projectId, @RequestParam(defaultValue = "true") Boolean active, @Valid @RequestBody AlarmItem projectAlarmItem) throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        alarmConfigQueryService.validAlarmItem(projectId, projectAlarmItem);
        return success(alarmConfigCmdService.add(projectId, projectAlarmItem, active));
    }

    @ApiOperation("查询项目所有配置项")
    @GetMapping("list")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<ProjectAlarmExt>> list(@PathVariable Long projectId) {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_READ);
        return success(alarmConfigQueryService.getProjectAlarms(projectId));
    }

    @ApiOperation("删除项目中某个预警配置项")
    @DeleteMapping("{alarmId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@PathVariable Long projectId, @PathVariable Long alarmId) throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        checkProjectHasAlarm(projectId, alarmId);
        alarmConfigCmdService.delete(alarmId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("编辑项目中某个预警配置项")
    @PutMapping("{alarmId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<ProjectAlarmExt> edit(@PathVariable Long projectId, @PathVariable Long alarmId, @Valid @RequestBody AlarmItem projectAlarmItem) throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        checkProjectHasAlarm(projectId, alarmId);
        alarmConfigQueryService.validAlarmItem(projectId, projectAlarmItem);
        return success(alarmConfigCmdService.update(projectId, alarmId, projectAlarmItem));
    }

    @ApiOperation("开启与关闭预警")
    @PutMapping("{alarmId:[0-9]+}/active")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse active(@PathVariable Long projectId, @PathVariable Long alarmId, @RequestParam(required = true) Boolean active) throws IOException {
        checkPermission(projectId, OperationType.PROJECT_MANAGE_UPDATE);
        checkProjectHasAlarm(projectId, alarmId);
        alarmConfigCmdService.active(alarmId, active);
        return SUCCESS_RESPONSE;
    }

    private void checkProjectHasAlarm(Long projectId, Long alarmId) {
        List<ProjectAlarmExt> projectAlarms = alarmConfigQueryService.getProjectAlarms(projectId);
        if (projectAlarms.stream().noneMatch(projectAlarm -> projectAlarm.getId().equals(alarmId))) {
            throw new CodedException(HttpStatus.NOT_FOUND, "在当前项目下未找到相应的预警设置！");
        }
    }
}
