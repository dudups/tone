package com.ezone.ezproject.modules.log.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.ezone.ezproject.es.entity.OperationLog;
import com.ezone.ezproject.es.entity.OperationLogField;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.log.bean.LogOperationType;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.DateTime;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Builder
public class ExcelOperationLogsExport {
    private List<OperationLog> operationLogs;
    private List<String> fields;
    private List<String> fieldNames;
    private ExcelWriter excelWriter;
    private WriteSheet writeSheet;
    private Map<String, String> logOperationTypeNames;

    public void writeExportExcel(OutputStream excel) {
        try {
            init(excel);
            ListUtils.partition(this.operationLogs, 100).forEach(logs -> logs.forEach(this::writeRow));
        } finally {
            if (null != this.excelWriter) {
                this.excelWriter.finish();
            }
        }
    }

    private void writeRow(OperationLog operationLog) {
        List<String> row = new ArrayList<>();
        for (String field : fields) {
            try {
                switch (field) {
                    case OperationLogField.CREATE_TIME:
                        row.add(DATE_TIME_FORMAT.apply(operationLog.getCreateTime()));
                        break;
                    case OperationLogField.OPERATOR:
                        row.add(operationLog.getOperator());
                        break;
                    case OperationLogField.IP:
                        row.add(operationLog.getIp());
                        break;
                    case OperationLogField.OPERATE_TYPE:
                        row.add(logOperationTypeNames.get(operationLog.getOperateType()));
                        break;
                    case OperationLogField.DETAIL:
                        row.add(operationLog.getDetail());
                        break;
                    case OperationLogField.PROJECT_ID:
                        row.add(operationLog.getProjectId().toString());
                        break;
                    default:
                }
            } catch (Exception e) {
                log.error("convert field to string value exception!", e);
                row.add(null);
            }
        }
        this.excelWriter.write(Arrays.asList(row), writeSheet);
    }

    private static final Function<Object, String> DATE_FORMAT = date -> {
        try {
            return new DateTime(date).toString("yyyy-MM-dd");
        } catch (Exception e) {
            log.error(String.format("format date exception:[%s]!", date), e.getMessage());
            return FieldUtil.toString(date);
        }
    };

    private static final Function<Object, String> DATE_TIME_FORMAT = date -> {
        try {
            return new DateTime(date).toString("yyyy-MM-dd HH:mm:ss");
        } catch (Exception e) {
            log.error(String.format("format date exception:[%s]!", date), e.getMessage());
            return FieldUtil.toString(date);
        }
    };

    private void init(OutputStream excel) {
        fields = Arrays.asList(OperationLogField.DEFAULT_SHOW_FIELDS);
        fieldNames = Arrays.asList(OperationLogField.DEFAULT_SHOW_FIELDS_NAME);
        this.excelWriter = EasyExcel.write(excel).registerWriteHandler(excelStyle()).build();
        this.writeSheet = EasyExcel.writerSheet("sheet1").build();
        this.excelWriter.write(Arrays.asList(fieldNames), writeSheet);
        logOperationTypeNames = Arrays.stream(LogOperationType.values())
                .collect(Collectors.toMap(Enum::name, LogOperationType::getCnName));
    }

    private CellWriteHandler excelStyle() {
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                         List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                         Integer relativeRowIndex, Boolean isHead) {
                Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                if (cell.getRowIndex() == 0) {
                    CellStyle style = workbook.createCellStyle();
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    cell.setCellStyle(style);
                }
            }
        };
    }
}
