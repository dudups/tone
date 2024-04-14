package com.ezone.ezproject.modules.query.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.CardQueryView;
import com.ezone.ezproject.dal.entity.enums.CardQueryViewType;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.query.bean.CopyCardQueryViewRequest;
import com.ezone.ezproject.modules.query.bean.CreateCardQueryViewRequest;
import com.ezone.ezproject.modules.query.service.CardQueryViewCmdService;
import com.ezone.ezproject.modules.query.service.CardQueryViewQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.List;

@ApiOperation("项目下卡片查询视图的相关操作")
@RestController
@RequestMapping("/project/card-query-view")
@Slf4j
@AllArgsConstructor
public class CardQueryViewController extends AbstractController {
    private CardQueryViewQueryService viewQueryService;

    private CardQueryViewCmdService viewCmdService;

    @ApiOperation("新建查询视图")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CardQueryView> create(@Valid @RequestBody CreateCardQueryViewRequest request)
            throws IOException {
        checkCreatePermission(request.getProjectId(), request.getType().name());
        return success(viewCmdService.create(request));
    }

    @ApiOperation("复制查询视图")
    @PostMapping("copy")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<CardQueryView> copy(@Valid @RequestBody CopyCardQueryViewRequest request)
            throws IOException {
        CardQueryView copyFrom = viewQueryService.select(request.getCopyFromId());
        if (null == copyFrom) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Template query-view not exist!");
        }
        checkCreatePermission(copyFrom.getProjectId(), request.getType().name());
        return success(viewCmdService.copy(request, copyFrom));
    }

    @ApiOperation("重命名")
    @PutMapping("{id:[0-9]+}/rename")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardQueryView> rename(@ApiParam(value = "查询视图ID", example = "1") @PathVariable Long id,
                                              @ApiParam(value = "查询视图名称") @RequestParam @Size(min = 1, max = 32) String name)
            throws IOException {
        CardQueryView view = viewQueryService.select(id);
        checkUpdatePermission(view);
        return success(viewCmdService.rename(view, name));
    }

    @ApiOperation("置顶")
    @PutMapping("{id:[0-9]+}/top")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardQueryView> top(@ApiParam(value = "查询视图ID", example = "1") @PathVariable Long id)
            throws IOException {
        CardQueryView view = viewQueryService.select(id);
        checkUpdatePermission(view);
        return success(viewCmdService.top(view));
    }

    @ApiOperation("更新查询视图详情")
    @PutMapping("{id:[0-9]+}/detail")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<CardQueryView> update(@ApiParam(value = "查询视图ID", example = "1") @PathVariable Long id,
                                              @ApiParam(value = "查询视图ID") @RequestBody SearchEsRequest request)
            throws IOException {
        CardQueryView view = viewQueryService.select(id);
        checkUpdatePermission(view);
        return success(viewCmdService.update(view, request));
    }

    @ApiOperation("查询查询视图列表")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<CardQueryView>> select(
            @ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
            @ApiParam(value = "查询视图类型") @RequestParam(required = false) CardQueryViewType type) throws IOException {
        String user = userService.currentUserName();
        checkHasProjectRead(projectId);
        if (CardQueryViewType.SHARE.equals(type)) {
            return success(viewQueryService.selectShareViews(projectId));
        } else {
            List<CardQueryView> views;
            if (CardQueryViewType.USER.equals(type)) {
                views = viewQueryService.selectUserViews(projectId, user);
            } else {
                views = viewQueryService.selectViews(projectId, user);
            }
            if (CollectionUtils.isEmpty(views)) {
                views = viewCmdService.initUserCardQueryViews(projectId, user);
            }
            return success(views);
        }
    }

    @ApiOperation("查询查询视图详情")
    @GetMapping("{id:[0-9]+}/detail")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<SearchEsRequest> selectDetail(@ApiParam(value = "查询视图ID", example = "1") @PathVariable Long id)
            throws IOException {
        CardQueryView view = viewQueryService.select(id);
        checkReadPermission(view);
        return success(viewQueryService.selectDetail(id));
    }

    @ApiOperation("删除查询视图")
    @DeleteMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse delete(@ApiParam(value = "查询视图ID", example = "1") @PathVariable Long id)
            throws IOException {
        CardQueryView view = viewQueryService.select(id);
        checkDeletePermission(view);
        viewCmdService.delete(view.getId());
        return SUCCESS_RESPONSE;
    }

    private void checkUpdatePermission(CardQueryView view) {
        String user = userService.currentUserName();
        if (CardQueryViewType.SHARE.name().equals(view.getType())) {
            checkPermission(view.getProjectId(), OperationType.CARD_VIEW_UPDATE);
        } else {
            if (!user.equals(view.getCreateUser())) {
                throw CodedException.FORBIDDEN;
            }
        }
    }

    private void checkDeletePermission(CardQueryView view) {
        String user = userService.currentUserName();
        if (CardQueryViewType.SHARE.name().equals(view.getType())) {
            checkPermission(view.getProjectId(), OperationType.CARD_VIEW_DELETE);
        } else {
            if (!user.equals(view.getCreateUser())) {
                throw CodedException.FORBIDDEN;
            }
        }
    }

    private void checkReadPermission(CardQueryView view) {
        String user = userService.currentUserName();
        if (CardQueryViewType.SHARE.name().equals(view.getType())) {
            checkHasProjectRead(view.getProjectId());
        } else {
            if (!user.equals(view.getCreateUser())) {
                throw CodedException.FORBIDDEN;
            }
        }
    }

    private void checkCreatePermission(Long projectId, String cardQueryViewType) {
        if (CardQueryViewType.SHARE.name().equals(cardQueryViewType)) {
            checkPermission(projectId, OperationType.CARD_VIEW_CREATE);
        } else {
            checkHasProjectRead(projectId);
        }
    }
}
