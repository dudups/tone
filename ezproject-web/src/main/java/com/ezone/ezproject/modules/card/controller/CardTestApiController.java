package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.modules.cli.EzTestCliService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ApiOperation("卡片")
@RestController
@RequestMapping("/project/card")
@Slf4j
@AllArgsConstructor
public class CardTestApiController extends AbstractCardController {
    private EzTestCliService ezTestCliService;

    @ApiOperation("更新卡片关联的测试接口")
    @PutMapping("{id:[0-9]+}/updateBindApis")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateBindApis(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                       @RequestParam Long spaceId,
                                       @RequestBody List<Long> apiIds) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());
        ezTestCliService.updateBindApis(id, spaceId, apiIds);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("解绑卡片关联的测试接口")
    @PostMapping("{id:[0-9]+}/unBindApis")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse unBindApis(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id,
                                   @RequestBody List<Long> apiIds) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkCanUpdateCard(card.getProjectId(), card.getId());
        ezTestCliService.unBindApis(id, apiIds);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询卡片关联的测试接口")
    @GetMapping("{id:[0-9]+}/listBindApis")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse listBindApis(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long id) {
        Card card = cardQueryService.select(id);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(card.getProjectId());
        return success(ezTestCliService.listBindApis(id));
    }
}
