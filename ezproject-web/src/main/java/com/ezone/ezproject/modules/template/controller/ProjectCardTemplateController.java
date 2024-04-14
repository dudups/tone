package com.ezone.ezproject.modules.template.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.template.service.ProjectCardTemplateService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@ApiOperation("项目卡片模版")
@RestController
@RequestMapping("/project/project/{id:[0-9]+}/card-template")
@Slf4j
@AllArgsConstructor
public class ProjectCardTemplateController extends AbstractController {
    private ProjectCardTemplateService projectCardTemplateService;

    @ApiOperation("设置项目卡片模版")
    @PutMapping
    @CheckAuthType(TokenAuthType.ALL)
    public BaseResponse update(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                               @ApiParam(value = "卡片类型key标识", example = "story") @RequestParam String cardType,
                               @ApiParam(value = "卡片模版json", example = "1") @RequestBody Map<String, Object> template)
            throws IOException {
        checkPermission(id, OperationType.PROJECT_MANAGE_UPDATE);
        projectCardTemplateService.setProjectCardTemplate(id, cardType, template);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询项目卡片模版")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<Map<String, Object>> select(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                         @ApiParam(value = "卡片类型key标识", example = "story") @RequestParam String cardType)
            throws IOException {
        checkHasProjectRead(id);
        return success(projectCardTemplateService.getProjectCardTemplate(id, cardType));
    }
}
