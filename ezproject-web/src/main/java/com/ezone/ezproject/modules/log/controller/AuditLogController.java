package com.ezone.ezproject.modules.log.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.es.entity.OperationLog;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.log.bean.LogOperationTypeSelectVo;
import com.ezone.ezproject.modules.log.service.OperationLogQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.bean.PageResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@ApiOperation("项目审计日志")
@RestController
@RequestMapping("/project/log")
@Slf4j
@AllArgsConstructor
public class AuditLogController extends AbstractController {
    private OperationLogQueryService operationLogQueryService;

    @ApiOperation("查询项目下审计日志")
    @PostMapping("{id:[0-9]+}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<OperationLog>> operationLogs(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                                          @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
                                                          @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                                          @RequestBody(required = false) List<Query> queries,
                                                          HttpServletResponse response) throws IOException {
        TotalBean<OperationLog> totalBean = operationLogQueryService.search(id, queries, pageNumber, pageSize);
        response.addHeader(PageResult.PAGE_TOTAL_COUNT, String.valueOf(totalBean.getTotal()));
        return success(totalBean.getList());
    }

    @ApiOperation("查询审计日志类型 key-名称映射")
    @GetMapping("operationTypes")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<LogOperationTypeSelectVo>> getAllLogOperationType() {
        return success(operationLogQueryService.getAllLogOptions());
    }

    @ApiOperation("导出审计日志")
    @PostMapping(path = "{id:[0-9]+}/export/excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @CheckAuthType(TokenAuthType.READ)
    public void importExcelTemplate(@ApiParam(value = "项目ID", example = "1") @PathVariable Long id,
                                    @RequestBody(required = false) List<Query> queries,
                                    HttpServletResponse response) throws IOException {
        checkHasProjectRead(id);
        response.setHeader("Content-disposition", "attachment;filename=export-opLogs.xlsx");
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        operationLogQueryService.writeExportExcel(id, queries, response.getOutputStream());
    }

}
