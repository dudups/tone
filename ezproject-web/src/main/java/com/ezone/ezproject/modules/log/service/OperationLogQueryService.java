package com.ezone.ezproject.modules.log.service;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.es.dao.OperationLogDao;
import com.ezone.ezproject.es.entity.OperationLog;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.log.bean.LogOperationType;
import com.ezone.ezproject.modules.log.bean.LogOperationTypeSelectVo;
import com.ezone.ezproject.modules.log.excel.ExcelOperationLogsExport;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class OperationLogQueryService {

    ProjectSchemaQueryService projectSchemaQueryService;

    private OperationLogDao operationLogDao;

    public void writeExportExcel(Long projectId, List<Query> queries, OutputStream excel)
            throws IOException {
        List<Query> finalQueries = new ArrayList<>(Collections.singletonList(Eq.builder().field("projectId").value(projectId.toString()).build()));
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        TotalBean<OperationLog> logTotalBean = operationLogDao.search(finalQueries, 1, 10000);
        ExcelOperationLogsExport.builder()
                .operationLogs(logTotalBean.getList())
                .build()
                .writeExportExcel(excel);
    }

    public TotalBean<OperationLog> search(Long projectId, List<Query> queries, Integer pageNumber, Integer pageSize) throws IOException {
        List<Query> finalQueries = new ArrayList<>(Collections.singletonList(Eq.builder().field("projectId").value(projectId.toString()).build()));
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        return operationLogDao.search(finalQueries, pageNumber, pageSize);
    }

    public List<LogOperationTypeSelectVo> getAllLogOptions() {
        List<LogOperationTypeSelectVo> typeDefines = new ArrayList<>();
        for (LogOperationType value : LogOperationType.values()) {
            typeDefines.add(LogOperationTypeSelectVo.builder().key(value).value(value.getCnName()).build());
        }
        return typeDefines;
    }

    public LogOperationType[] allLogOperationTypes() {
        LogOperationType[] values = LogOperationType.values();
        return values;
    }
}
