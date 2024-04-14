package com.ezone.ezproject.modules.card.controller;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.CheckCardRequest;
import com.ezone.ezproject.modules.card.bean.FilterCardRequest;
import com.ezone.ezproject.modules.card.bean.GetCardIdsByKeysRequest;
import com.ezone.ezproject.modules.card.bean.ListCardByKeysRequest;
import com.ezone.ezproject.modules.card.bean.ListCardRequest;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.SeqNumOrTitle;
import com.ezone.ezproject.modules.card.bpm.bean.StatusFlowResult;
import com.ezone.ezproject.modules.card.bpm.service.CardWorkloadBpmCmdService;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.service.CardReferenceValueHelper;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectRepoService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApiOperation("卡片")
@RestController
@RequestMapping("/project/api/card")
@Slf4j
@AllArgsConstructor
public class CardApiController extends AbstractController {
    private CardQueryService cardQueryService;
    private CardSearchService cardSearchService;
    private ProjectRepoService projectRepoService;
    private ProjectMemberQueryService projectMemberQueryService;
    private CardCmdService cardCmdService;
    private CardWorkloadBpmCmdService cardWorkloadBpmCmdService;

    private CardReferenceValueHelper cardReferenceValueHelper;

    @ApiOperation("过滤返回合法&有权限的卡片列表")
    @GetMapping("filter")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<String>> filter(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                                             @ApiParam(value = "用户名", example = "u1") @RequestParam String user,
                                             @ApiParam(value = "卡片keys", example = "demo-1") @RequestParam List<String> cardKeys) {
        return success(cardQueryService.filter(companyId, user, cardKeys));
    }

    @ApiOperation("过滤返回合法&有权限的卡片列表")
    @PostMapping("filter")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<String>> filterV2(@Valid @RequestBody FilterCardRequest request) {
        return success(cardQueryService.filter(request.getCompanyId(), request.getUser(), request.getCardKeys()));
    }

    @ApiOperation("判断合法&有权限&关联代码库")
    @GetMapping("{projectKey:.+}-{seqNum:[0-9]+}/check")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse check(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                              @ApiParam(value = "用户名", example = "u1") @RequestParam String user,
                              @Deprecated @RequestParam(required = false) String repo,
                              @RequestParam(required = false, defaultValue = "0") Long repoId,
                              @ApiParam(value = "项目标示", example = "p1") @PathVariable String projectKey,
                              @ApiParam(value = "卡片编号", example = "1") @PathVariable Long seqNum) {
        Card card = cardQueryService.select(companyId, projectKey, seqNum);
        if (null == card) {
            throw CodedException.NOT_FOUND;
        }
        if (!projectMemberQueryService.isUserInProject(companyId, card.getProjectId(), user)) {
            throw new CodedException(HttpStatus.FORBIDDEN, "非项目成员");
        }
        if (repoId > 0) {
            projectRepoService.checkIsBindRepo(card.getProjectId(), repoId);
        } else {
            projectRepoService.checkIsBindRepo(card.getProjectId(), repo);
        }
        projectRepoService.checkIsBindRepo(card.getProjectId(), repoId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("检查卡片:权限&存在&在项目下")
    @GetMapping("check")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse check(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                              @ApiParam(value = "用户名", example = "u1") @RequestParam String user,
                              @ApiParam(value = "卡片ids", example = "1") @RequestParam List<Long> cardIds) {
        checkHasProjectRead(user, projectId);
        cardQueryService.check(projectId, cardIds);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("检查卡片:权限&存在&在项目下")
    @PostMapping("check")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse checkV2(@Valid @RequestBody CheckCardRequest request) {
        checkHasProjectRead(request.getUser(), request.getProjectId());
        cardQueryService.check(request.getProjectId(), request.getCardIds());
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("获取卡片信息")
    @GetMapping("list")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<TotalBean<CardBean>> list(@ApiParam(value = "卡片ids", example = "1") @RequestParam List<Long> cardIds,
                                                  @ApiParam(value = "卡片字段", example = "title") @RequestParam String[] fields,
                                                  @RequestParam(required = false) boolean excludeDeleted) throws Exception {
        TotalBean<CardBean> totalBean = cardSearchService.selectDetail(cardIds, excludeDeleted, fields);
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), fields);
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("获取卡片信息")
    @PostMapping("list")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<TotalBean<CardBean>> listV2(@RequestBody ListCardRequest request) throws Exception {
        TotalBean<CardBean> totalBean = cardSearchService.selectDetail(request.getCardIds(), request.isExcludeDeleted(), request.getFields());
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), request.getFields());
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("获取卡片信息")
    @GetMapping("list/byKeys")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<TotalBean<CardBean>> listByKeys(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                                                        @ApiParam(value = "卡片ids", example = "1") @RequestParam List<String> cardKeys,
                                                        @ApiParam(value = "卡片字段", example = "title") @RequestParam String[] fields) throws Exception {
        TotalBean<CardBean> totalBean = cardSearchService.selectDetailByKeys(companyId, cardKeys, fields);
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), fields);
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("获取卡片信息")
    @PostMapping("list/byKeys")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<TotalBean<CardBean>> listByKeysV2(@Valid @RequestBody ListCardByKeysRequest request) throws Exception {
        TotalBean<CardBean> totalBean = cardSearchService.selectDetailByKeys(request.getCompanyId(), request.getCardKeys(), request.getFields());
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), request.getFields());
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("获取卡片信息")
    @PutMapping("query/byKeys")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse artifactRepoProject(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                                            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                            @ApiParam(value = "卡片ids", example = "1") @RequestParam List<String> cardKeys,
                                            @ApiParam(value = "查询条件") @RequestBody SearchEsRequest searchCardRequest) throws IOException {
        TotalBean<CardBean> totalBean;
        if (cardKeys == null) {
            totalBean = new TotalBean();
            totalBean.setTotal(0);
            totalBean.setList(Collections.emptyList());
            return success(totalBean);
        }
        totalBean = cardSearchService.selectDetailByKeys(companyId, cardKeys, searchCardRequest, true, pageNumber, pageSize);
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), searchCardRequest.getFields());
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("获取卡片ID")
    @GetMapping("getCardIdsByKeys")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Long>> getCardIdsByKeys(@ApiParam(value = "公司ID", example = "1") @RequestParam Long companyId,
                                                     @ApiParam(value = "卡片ids", example = "1") @RequestParam List<String> cardKeys) {
        return success(cardQueryService.selectIdsByKeys(companyId, cardKeys));
    }

    @ApiOperation("获取卡片ID")
    @PostMapping("getCardIdsByKeys")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<List<Long>> getCardIdsByKeysV2(@Valid @RequestBody GetCardIdsByKeysRequest request) {
        return success(cardQueryService.selectIdsByKeys(request.getCompanyId(), request.getCardKeys()));
    }

    @ApiOperation("查询项目下卡片")
    @GetMapping("searchByProject")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse<TotalBean<CardBean>> searchByProject(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                             @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                             @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                             @ApiParam(value = "查询词/卡片编号") @RequestParam(required = false) String q,
                                                             @ApiParam(value = "卡片字段", example = "title") @RequestParam(required = false) String[] fields,
                                                             HttpServletResponse response) throws IOException {
        String[] showFields = fields == null || fields.length == 0 ? new String[]{CardField.SEQ_NUM, CardField.TITLE} : fields;
        TotalBean<CardBean> totalBean = cardSearchService.search(
                projectId,
                SearchEsRequest.builder()
                        .queries(Arrays.asList(SeqNumOrTitle.builder().values(q).build()))
                        .sorts(Arrays.asList(SearchEsRequest.Sort.builder().field(CardField.SEQ_NUM).order(SortOrder.DESC).build()))
                        .fields(showFields)
                        .build(),
                false,
                pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        cardReferenceValueHelper.tryLoadProjectSchemas(totalBean.getRefs(), fields);
        cardReferenceValueHelper.tryLoadProjectAlarm(totalBean.getRefs());
        return success(totalBean);
    }

    @ApiOperation("审批单结果回调")
    @PostMapping("{cardId:[0-9]+}/status/flow/result")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse statusBpmFlow(
            @ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
            @ApiParam(value = "审批流ID", example = "1") @RequestParam Long flowId,
            @ApiParam(value = "审批流结果是否通过", example = "1") @RequestParam boolean approved
    ) throws IOException {
        cardCmdService.tryBpmStatusFlow(cardId, flowId, approved);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("审批单结果回调-批量")
    @PostMapping("status/flow/results")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse statusBpmFlows(@RequestParam Long projectId, @RequestBody StatusFlowResult[] flowResults) throws IOException {
        if (flowResults.length == 0) {
            return SUCCESS_RESPONSE;
        }
        if (flowResults.length > 500) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "批量处理最大支持500个");
        }
        cardCmdService.tryBpmStatusFlows(flowResults, projectId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("审批单结果回调")
    @PostMapping("{cardId:[0-9]+}/workload/flow/result")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse workloadBpmFlow(
            @ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
            @ApiParam(value = "审批流ID", example = "1") @RequestParam Long flowId,
            @ApiParam(value = "审批流结果是否通过", example = "1") @RequestParam boolean approved
    ) throws IOException {
        Card card = cardQueryService.select(cardId);
        cardWorkloadBpmCmdService.updateIncrWorkloadBpmFlowResult(card, flowId, approved,
                (cardDetail, workload) -> cardCmdService.incrActualWorkload(card, cardDetail, workload.getIncrHours()));
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("审批单结果回调-批量")
    // @PostMapping("workload/flow/results")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse workloadBpmFlows(@RequestParam Long projectId, @RequestBody StatusFlowResult[] flowResults) throws IOException {
        if (flowResults.length == 0 ) {
            return SUCCESS_RESPONSE;
        }
        if (flowResults.length > 500) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "批量处理最大支持500个");
        }
        // todo cardCmdService.tryBpmStatusFlows(flowResults, projectId);
        return SUCCESS_RESPONSE;
    }
    //////////

    @ApiOperation("通过bug分组，计算每组bug中已经完成的bug数。")
    @PostMapping("countBugEnd")
    @InternalApiAuthentication
    @ApiImplicitParams({
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_TIMESTAMP, required = true, dataType = "Long", paramType = "header", value = "milliseconds"),
            @ApiImplicitParam(name = InternalApiAuthentication.HEADER_MD5, required = true, dataType = "String", paramType = "header", value = "md5(拼接约定token+timestamp)")
    })
    public BaseResponse countBugEnd(
            @ApiParam(value = "key:分组ID，value:bug id集合", example = "1") @RequestBody Map<Long, List<Long>> planningBindBugIds) throws IOException {
        Map<Long, Long> resultMap = new HashMap<>();
        Set<Long> ids = new HashSet<>();

        planningBindBugIds.forEach((key, value) -> ids.addAll(value));
        Map<Long, Map<String, Object>> cardMap = cardQueryService.selectDetail(new ArrayList<>(ids), CardField.CALC_IS_END);
        planningBindBugIds.forEach((key, bugIds) -> {
            Long endCount = 0L;
            for (Long bugId : bugIds) {
                Map<String, Object> cardDetail = cardMap.get(bugId);
                if (cardDetail != null && CardHelper.isEnd(cardDetail)) {
                    endCount++;
                }
            }
            resultMap.put(key, endCount);
        });
        return success(resultMap);
    }
}
