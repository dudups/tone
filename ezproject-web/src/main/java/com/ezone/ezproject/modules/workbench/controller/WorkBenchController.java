package com.ezone.ezproject.modules.workbench.controller;

import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.controller.AbstractCardController;
import com.ezone.ezproject.modules.workbench.service.WorkBenchService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;


@ApiOperation("工作台")
@RestController
@RequestMapping("/project/workbench")
@Slf4j
@AllArgsConstructor
public class WorkBenchController extends AbstractCardController {

    private WorkBenchService workBenchService;

    @ApiOperation("保存我的任务的搜索条件")
    @PostMapping("searchCardRequest")
    public BaseResponse<SearchEsRequest> searchCardRequest(@RequestBody SearchEsRequest searchCardRequest) throws IOException {
        Long userId = userService.currentUserId();
        List<Query> queries = searchCardRequest.getQueries();
        if (queries.isEmpty()) {
            searchCardRequest = workBenchService.getSearchCardRequest(userId);
        }
        workBenchService.cacheUserQuery(userId, searchCardRequest);
        if (searchCardRequest.getFields() == null || searchCardRequest.getFields().length == 0) {
            searchCardRequest.setFields(WorkBenchService.DEFAULT_CARD_FIELD);
        }
        return success(searchCardRequest);
    }

    @ApiOperation("保存我的任务的搜索条件")
    @PostMapping("clearAllRequestCache")
    public BaseResponse<SearchEsRequest> clearAllRequestCache(@RequestBody SearchEsRequest searchCardRequest) throws IOException {
        checkIsCompanyAdmin();
        workBenchService.clearAllCache();
        return success(searchCardRequest);
    }
}