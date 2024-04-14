package com.ezone.ezproject.modules.card.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.AbstractCellWriteHandler;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.storage.IStorage;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.check.ActualWorkloadChecker;
import com.ezone.ezproject.modules.card.field.check.IFieldChecker;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.ProjectCardDailyLimiter;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.event.events.CardsCreateEvent;
import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ExcelCardImport {
    private UserProjectPermissions permissions;
    private Project project;
    private ProjectCardSchema schema;
    private List<String> cardTypes;
    private ProjectWorkloadSetting workloadSetting;
    private OperationContext opContext;
    private Consumer<Map<String, Object>> setCalcFields;
    private int batchSize;
    private CardMapper cardMapper;
    private CardDao cardDao;
    private CardHelper cardHelper;
    private Function<String, Plan> findPlanByName;
    private EventDispatcher eventDispatcher;
    private IStorage storage;
    private ProjectCardDailyLimiter projectCardDailyLimiter;
    private CompanyCardSchema companyCardSchema;

    // init with head row
    private Map<Integer, CardField> fields;
    private Map<String, List<Integer>> cardTypeRequiredFieldIndexes;
    private Integer errorIndex;
    private Map<String, Map<String, String>> cardTypeStatusNameKeyMap;
    private Map<String, String> typeNameKeyMap;
    private int errorRowsCount;
    private String errorStoragePath;
    private File errorTmpFile;
    private ExcelWriter errorExcelWriter;
    private WriteSheet errorWriteSheet;


    /**
     * 由于数据分批导入，且父子关系必须相连，此集合用于存储批量中最后一行
     * 及父子关系中父编号对应行的数据。
     */
    private Map<String, Card> parentNumCardMap = new HashMap<>();
    private boolean excelColumnIncludeCardType = false;

    public static final String STORAGE_TMP_ERROR_PATH = ".tmp.error";

    //编号及父卡片编号列位置
    public static final int TITLE_CARD_TEMP_NUM_INDEX = 0;
    public static final int TITLE_PARENT_CARD_TEMP_NUM_INDEX = 1;
    public static final String TITLE_FIELD_CARD_TEMP_NUM = "编号";
    public static final String TITLE_FIELD_PARENT_CASE_NUM = "父卡片编号";
    private static final List<String> COMMON_REQUIRED_FIELD_KEYS = Arrays.asList(CardField.TITLE, CardField.STATUS);

    @Builder
    public ExcelCardImport(UserProjectPermissions permissions,
                           Project project, ProjectCardSchema schema, ProjectWorkloadSetting workloadSetting,
                           List<String> cardTypes, OperationContext opContext, int batchSize,
                           CardMapper cardMapper, CardDao cardDao, CardHelper cardHelper,
                           Consumer<Map<String, Object>> setCalcFields,
                           Function<String, Plan> findPlanByName, EventDispatcher eventDispatcher,
                           IStorage storage,
                           ProjectCardDailyLimiter projectCardDailyLimiter,
                           CompanyCardSchema companyCardSchema) {
        this.permissions = permissions;
        this.project = project;
        this.schema = schema;
        this.cardTypes = cardTypes;
        this.opContext = opContext;
        this.setCalcFields = setCalcFields;
        this.batchSize = batchSize;
        this.cardMapper = cardMapper;
        this.cardDao = cardDao;
        this.cardHelper = cardHelper;
        this.findPlanByName = findPlanByName;
        this.eventDispatcher = eventDispatcher;
        this.storage = storage;
        this.projectCardDailyLimiter = projectCardDailyLimiter;
        this.companyCardSchema = companyCardSchema;
    }

    public Result importExcel(InputStream excel) {
        try {
            EasyExcel.read(excel, new CardListener(this)).sheet(0).doRead();
        } finally {
            clear();
        }
        return Result.builder().errorRowsCount(errorRowsCount).errorStoragePath(errorStoragePath).build();
    }

    private void invokeHeadMap(Map<Integer, String> headMap) {
        this.fields = new HashMap<>();
        this.errorIndex = headMap.size();
        Map<String, CardField> enabledFieldNameMap = schema.getFields().stream()
                .filter(f -> SysFieldOpLimit.canOp(f, SysFieldOpLimit.Op.IMPORT))
                .collect(Collectors.toMap(CardField::getName, f -> f));
        headMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String headValue = entry.getValue();
            CardField cardField = enabledFieldNameMap.get(headValue);
            if (cardField != null) {
                this.fields.put(entry.getKey(), cardField);
            }
        });
        this.cardTypeRequiredFieldIndexes = new HashMap<>();

        if (this.fields.values().stream().anyMatch(field -> field.getKey().equals(CardField.TYPE))) {
            this.excelColumnIncludeCardType = true;
        } else {
            if (this.cardTypes.isEmpty()) {
                throw new CustomerExcelAnalysisException(HttpStatus.BAD_REQUEST, "您导入时未选择卡片类型，但在您上传的excel中也未检测到卡片类型]这一列。请下载模板填写数据后在导入");
            } else if (this.cardTypes.size() > 1) {
                throw new CustomerExcelAnalysisException(HttpStatus.BAD_REQUEST, "您选择导入多个卡片类型，但在您上传的excel中未检测到卡片类型这一列。请下载模板填写数据后在导入");
            }
        }
        Set<String> lackedRequiredFieldNames = new HashSet<>();
        this.cardTypeStatusNameKeyMap = new HashMap<>();
        this.cardTypes.forEach(cardType -> {
            List<String> requiredFieldKeys = new ArrayList<>(COMMON_REQUIRED_FIELD_KEYS);
            CardType cardTypeObj = schema.findCardType(cardType);
            if (cardTypeObj == null) {
                throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE, "非法卡片类型!");
            }
            requiredFieldKeys.addAll(cardTypeObj.findRequiredFields("open"));
            requiredFieldKeys.forEach(key -> {
                for (int i = 0; i < this.fields.size(); i++) {
                    CardField field = this.fields.get(i);
                    if (field != null && key.equals(field.getKey())) {
                        List<Integer> requiredFieldIndexes = this.cardTypeRequiredFieldIndexes.getOrDefault(cardType, new ArrayList<>());
                        requiredFieldIndexes.add(i);
                        this.cardTypeRequiredFieldIndexes.put(cardType, requiredFieldIndexes);
                        return;
                    }
                }
                lackedRequiredFieldNames.add(schema.findCardField(key).getName());
            });
            List<String> enabledStatusKeys = cardTypeObj.getStatuses().stream().map(CardType.StatusConf::getKey).collect(Collectors.toList());
            this.cardTypeStatusNameKeyMap.put(cardType, schema.getStatuses().stream()
                    .filter(s -> enabledStatusKeys.contains(s.getKey()))
                    .collect(Collectors.toMap(CardStatus::getName, CardStatus::getKey)));
        });

        if (CollectionUtils.isNotEmpty(lackedRequiredFieldNames)) {
            throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE,
                    String.format("缺少字段：[%s]", StringUtils.join(lackedRequiredFieldNames, ",")));
        }

        this.typeNameKeyMap = companyCardSchema.getTypes().stream().collect(Collectors.toMap(CompanyCardTypeConf::getName, CompanyCardTypeConf::getKey));
        this.errorRowsCount = 0;
        String errorFileName = StringUtils.joinWith("-", project.getKey(), "upload-fail.xlsx");
        String uuid = UUID.randomUUID().toString();
        String localErrorFileName = StringUtils.joinWith("-", uuid, errorFileName);
        this.errorStoragePath = StringUtils.joinWith("/", STORAGE_TMP_ERROR_PATH, uuid, errorFileName);
        this.errorTmpFile = new File(StringUtils.joinWith("/", System.getProperty("java.io.tmpdir"), localErrorFileName));
        this.errorExcelWriter = EasyExcel.write(this.errorTmpFile).registerWriteHandler(errorExcelStyle(this.errorIndex)).build();
        this.errorWriteSheet = EasyExcel.writerSheet("sheet1").build();
        this.writeErrorHead(headMap);
    }

    private void invokeRows(List<Map<Integer, String>> rows) {
        Long seqNum = cardHelper.seqNums(project.getId(), rows.size());
        List<String> ranks = cardHelper.nextRanks(project.getId(), rows.size());
        //cardTempNumCards key-excel中编号列。
        Map<String, Card> cardTempNumCards = new HashMap<>();
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        IFieldChecker workloadChecker = ActualWorkloadChecker.builder()
                .workloadSetting(workloadSetting)
                .build();
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            Map<String, Object> cardDetail;
            try {
                projectCardDailyLimiter.check(project.getId());
                cardDetail = convertToCard(row);
                permissions.checkLimitPermission(OperationType.CARD_CREATE, cardDetail);
                workloadChecker.check(cardDetail);
            } catch (CustomerExcelAnalysisException e) {
                writeErrorRow(row, e.getMessage());
                continue;
            }
            //设置父卡片ID
            String parentCardTempNum = row.get(ExcelCardImport.TITLE_PARENT_CARD_TEMP_NUM_INDEX);
            if (StringUtils.isNotEmpty(parentCardTempNum)) {
                Card parentCard = this.parentNumCardMap.get(parentCardTempNum);
                if (parentCard == null) {
                    parentCard = cardTempNumCards.get(parentCardTempNum);
                }
                if (parentCard == null) {
                    writeErrorRow(row, "未找到" + TITLE_FIELD_PARENT_CASE_NUM);
                    continue;
                }
                cardDetail.put(CardField.PARENT_ID, parentCard.getId());
                this.parentNumCardMap.put(parentCardTempNum, parentCard);
            }
            Card card = newCard(cardDetail, seqNum++, ranks.get(i));

            cardMapper.insert(card);
            CardHelper.setCardCreatedProps(cardDetail, opContext, card);
            cardDetails.put(card.getId(), cardDetail);
            String cardTempNum = row.get(ExcelCardImport.TITLE_CARD_TEMP_NUM_INDEX);
            if (i == rows.size() - 1) {
                this.parentNumCardMap.put(cardTempNum, card);
            }
            cardTempNumCards.put(cardTempNum, card);
        }
        if (MapUtils.isNotEmpty(cardDetails)) {
            try {
                cardDao.saveOrUpdate(cardDetails);
            } catch (IOException e) {
                log.error("CardDao saveOrUpdate exception!", e);
                throw new CustomerExcelAnalysisException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
            eventDispatcher.dispatch(CardsCreateEvent.builder().user(opContext.getUserName()).cardDetails(cardDetails).build());
        }
    }

    private Map<String, Object> convertToCard(Map<Integer, String> row) throws CustomerExcelAnalysisException {
        Map<String, Object> cardDetail = new HashMap<>();
        boolean isPlanProcessed = false;
        Plan plan;
        for (Map.Entry<Integer, String> entry : row.entrySet()) {
            CardField field = fields.get(entry.getKey());
            if (field == null) {
                continue;
            }
            isPlanProcessed = isPlanProcessed || CardField.PLAN_ID.equals(field.getKey());
            Object value = convertFieldStringValue(field, entry.getValue());
            if (null != value) {
                cardDetail.put(field.getKey(), value);
                if (CardField.PLAN_ID.equals(field.getKey())) {
                    plan = findPlanByName.apply(entry.getValue());
                    if (plan == null) {
                        throw new CustomerExcelAnalysisException(HttpStatus.NOT_FOUND, String.format("计划[%s]不存在！", value));
                    }
                }
            }
        }
        String rowCardType;
        if (this.excelColumnIncludeCardType) {
            rowCardType = FieldUtil.getType(cardDetail);
        } else {
            String cardType = cardTypes.get(0);
            cardDetail.put(CardField.TYPE, cardType);
            rowCardType = cardType;
        }
        CardType cardTypeObj = schema.findCardType(rowCardType);
        if (null == cardTypeObj || !cardTypeObj.isEnable()) {
            throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE, "非法卡片类型!");
        }

        Map<String, String> statusNameKeyMap = cardTypeStatusNameKeyMap.get(rowCardType);
        if (statusNameKeyMap == null) {
            List<String> enabledStatusKeys = cardTypeObj.getStatuses().stream().map(CardType.StatusConf::getKey).collect(Collectors.toList());
            statusNameKeyMap = schema.getStatuses().stream()
                    .filter(s -> enabledStatusKeys.contains(s.getKey()))
                    .collect(Collectors.toMap(CardStatus::getName, CardStatus::getKey));
            this.cardTypeStatusNameKeyMap.put(rowCardType, statusNameKeyMap);
        }
        final Map<String, String> finalStatusNameKeyMap = statusNameKeyMap;
        fields.entrySet().stream().filter(entry -> entry.getValue().getKey().equals(CardField.STATUS)).findFirst().ifPresent(entity -> {
                    String status = finalStatusNameKeyMap.get(row.get(entity.getKey()));
                    if (null == status) {
                        throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE, "非法状态！");
                    }
                    cardDetail.put(CardField.STATUS, status);
                }
        );

        if (!isPlanProcessed) {
            plan = findPlanByName.apply(null);
            if (null != plan) {
                cardDetail.put(CardField.PLAN_ID, plan.getId());
            }
        }

        List<String> lackedRequiredFieldNames = new ArrayList<>();
        List<Integer> requiredFieldIndexes = this.cardTypeRequiredFieldIndexes.get(cardTypeObj.getKey());
        //处理excel中有导入时未选择的卡片类型，这里作兼容处理，允许导入。
        if (requiredFieldIndexes == null) {
            List<String> requiredFieldKeys = cardTypeObj.findRequiredFields("open");
            requiredFieldKeys.addAll(COMMON_REQUIRED_FIELD_KEYS);
            requiredFieldKeys.forEach(key -> {
                for (int i = 0; i < this.fields.size(); i++) {
                    CardField field = this.fields.get(i);
                    if (field != null && key.equals(field.getKey())) {
                        List<Integer> tempRequiredFieldIndexes = this.cardTypeRequiredFieldIndexes.getOrDefault(cardTypeObj.getKey(), new ArrayList<>());
                        tempRequiredFieldIndexes.add(i);
                        this.cardTypeRequiredFieldIndexes.put(cardTypeObj.getKey(), tempRequiredFieldIndexes);
                    }
                }
            });
            requiredFieldIndexes = this.cardTypeRequiredFieldIndexes.get(cardTypeObj.getKey());
        }

        requiredFieldIndexes.forEach(index -> {
            if (FieldUtil.isEmptyValue(row.get(index))) {
                lackedRequiredFieldNames.add(fields.get(index).getName());
            }
        });
        if (CollectionUtils.isNotEmpty(lackedRequiredFieldNames)) {
            throw new CustomerExcelAnalysisException(HttpStatus.NOT_ACCEPTABLE,
                    String.format("缺少字段：[%s]", StringUtils.join(lackedRequiredFieldNames, ",")));
        }
        //类型字段验证，去掉非本卡片类型字段值。
        List<String> enableKeys = cardTypeObj.getFields().stream().filter(CardType.FieldConf::isEnable).map(CardType.FieldConf::getKey).collect(Collectors.toList());
        cardDetail.keySet().removeIf(key -> !enableKeys.contains(key));
        setCalcFields.accept(cardDetail);
        return cardDetail;
    }

    private Card newCard(Map<String, Object> cardDetail, Long seqNum, String rank) {
        Long planId = FieldUtil.toLong(cardDetail.get(CardField.PLAN_ID));
        Long parentId = FieldUtil.toLong(cardDetail.get(CardField.PARENT_ID));
        Long ancestorId = 0L;
        if (parentId > 0L) {
            Card parent = cardMapper.selectByPrimaryKey(parentId);
            if (null != parent && parent.getProjectId().equals(project.getId())) {
                ancestorId = parent.getAncestorId() > 0 ? parent.getAncestorId() : parentId;
            } else {
                parentId = 0L;
            }
        }
        return Card.builder()
                .id(IdUtil.generateId())
                .projectId(project.getId())
                .projectKey(project.getKey())
                .seqNum(seqNum)
                .planId(planId)
                .parentId(parentId)
                .ancestorId(ancestorId)
                .rank(rank)
                .companyId(project.getCompanyId())
                .deleted(false)
                .maxCommentSeqNum(0L)
                .storyMapNodeId(0L)
                .latestEventId(0L)
                .build();
    }

    private void writeErrorRow(Map<Integer, String> row, String error) {
        this.errorRowsCount++;
        List<String> values = new ArrayList<>();
        for (int i = 0; i < this.errorIndex; i++) {
            values.add(row.get(i));
        }
        values.add(error);
        errorExcelWriter.write(Arrays.asList(values), errorWriteSheet);
    }

    private void writeErrorHead(Map<Integer, String> headMap) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < this.errorIndex; i++) {
            values.add(headMap.get(i));
        }
        values.add("失败原因");
        errorExcelWriter.write(Arrays.asList(values), errorWriteSheet);
    }

    private void clear() {
        if (null != errorTmpFile && errorTmpFile.exists()) {
            if (null != errorExcelWriter) {
                try {
                    errorExcelWriter.finish();
                } catch (Exception e) {
                    log.error("errorExcelWriter finish exception!", e);
                }
            }
            if (errorRowsCount > 0) {
                try {
                    try (FileInputStream errFileInputStream = new FileInputStream(errorTmpFile)) {
                        storage.save(errorStoragePath, errFileInputStream, errorTmpFile.length());
                    }
                } catch (Exception e) {
                    log.error("storage save errorTmpFile exception!", e);
                }
            }
            try {
                FileUtils.forceDelete(errorTmpFile);
            } catch (Exception e) {
                log.error("forceDelete errorTmpFile exception!", e);
            }
        }
    }

    public static final String IMPORT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String IMPORT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String IMPORT_DATE_FORMAT_EXAMPLE = "2022-10-21";
    public static final String IMPORT_DATE_TIME_FORMAT_EXAMPLE = "2022-10-21 18:30:00";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern(IMPORT_DATE_FORMAT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern(IMPORT_DATE_TIME_FORMAT);

    private Object convertFieldStringValue(CardField field, String value) {
        if (field == null) {
            return null;
        }
        try {
            switch (field.getKey()) {
                case CardField.PLAN_ID:
                    Plan plan = findPlanByName.apply(value);
                    if (null == plan) {
                        return 0L;
                    }
                    return plan.getId();
                case CardField.PARENT_ID:
                case CardField.STORY_MAP_NODE_ID:
                    return 0L;
                case CardField.TYPE:
                    return typeNameKeyMap.get(value);
                default:
                    switch (field.getValueType()) {
                        case BOOLEAN:
                            return StringUtils.equalsIgnoreCase(value, "true");
                        case STRINGS:
                            return StringUtils.isEmpty(value) ? null : Arrays.asList(StringUtils.split(value, FieldUtil.JOIN_SEPARATOR));
                        case FLOAT:
                            return NumberUtils.toFloat(value, 0F);
                        case LONG:
                            return NumberUtils.toLong(value, 0L);
                        case LONGS:
                            return StringUtils.isEmpty(value) ? null : Arrays.stream(StringUtils.split(value, FieldUtil.JOIN_SEPARATOR)).map(NumberUtils::toLong).collect(Collectors.toList());
                        case DATE:
                            if (FieldType.DATE_TIME.equals(field.getType())) {
                                return value == null ? null : DATE_TIME_FORMATTER.parseDateTime(value).getMillis();
                            } else {
                                return value == null ? null : DATE_FORMATTER.parseDateTime(value).getMillis();
                            }
                        case STRING:
                            return getAndValidStringValue(field, value);
                        default:
                            return value;
                    }
            }
        } catch (Exception e) {
            log.error("convertFieldStringValue exception!", e);
            return null;
        }
    }

    private Object getAndValidStringValue(CardField field, String value) {
        switch (field.getType()) {
            case SELECT:
            case RADIO:
                return FieldUtil.checkNameAndGetKeyInOptions(value, field);
            case CHECK_BOX:
                return FieldUtil.checkAllAndGetKeysInOptions(value, field);
            default:
                return value;
        }
    }

    private CellWriteHandler errorExcelStyle(int errorIndex) {
        return new AbstractCellWriteHandler() {
            @Override
            public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                         List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                         Integer relativeRowIndex, Boolean isHead) {
                Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                if (cell.getColumnIndex() == errorIndex) {
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    cellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
                    cell.setCellStyle(cellStyle);
                } else if (cell.getRowIndex() == 0) {
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    cellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    cell.setCellStyle(cellStyle);
                }
            }
        };
    }

    @Slf4j
    public static class CardListener extends AnalysisEventListener<Map<Integer, String>> {
        private ExcelCardImport excelCardImport;

        private List<Map<Integer, String>> rows = new ArrayList<>();

        @Builder
        public CardListener(ExcelCardImport excelCardImport) {
            this.excelCardImport = excelCardImport;
        }

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            excelCardImport.invokeHeadMap(headMap);
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            rows.add(data);
            if (rows.size() >= excelCardImport.batchSize) {
                excelCardImport.invokeRows(rows);
                rows.clear();
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (CollectionUtils.isNotEmpty(rows)) {
                excelCardImport.invokeRows(rows);
            }
        }
    }

    @Data
    @Builder
    public static class Result {
        private int errorRowsCount;
        private String errorStoragePath;
    }

}
