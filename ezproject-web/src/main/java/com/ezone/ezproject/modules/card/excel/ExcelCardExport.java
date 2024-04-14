package com.ezone.ezproject.modules.card.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.AbstractCellWriteHandler;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.template.VelocityTemplate;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.velocity.VelocityContext;
import org.joda.time.DateTime;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ExcelCardExport {
    private String companyName;
    private CompanyCardSchema companySchema;
    private Project project;
    private ProjectCardSchema schema;
    private List<Card> cards;
    private List<CardField> fields;
    private boolean exportOperations;
    private CardDao cardDao;
    private Function<List<Long>, List<Card>> findCardByIds;
    private Function<List<Long>, List<Plan>> findPlanByIds;
    private Function<List<Long>, List<StoryMapNode>> findStoryMapNodeByIds;
    private Function<List<Long>, Map<Long, List<String>>> findRelateCardKeys;
    private Function<Long, List<CardEvent>> findEventByCardId;
    private Function<Card, String> generateCardUrl;

    // init
    private Map<Long, String> parentKeys = MapUtils.EMPTY_MAP;
    private Map<Long, List<String>> relatedCardKeys = MapUtils.EMPTY_MAP;
    private Map<Long, String> planNames = MapUtils.EMPTY_MAP;
    private Map<Long, String> storyMapNodeNames = MapUtils.EMPTY_MAP;
    private Map<String, String> statusNames = MapUtils.EMPTY_MAP;
    private Map<String, String> typeNames = MapUtils.EMPTY_MAP;
    private ExcelWriter excelWriter;
    private WriteSheet writeSheet;
    private EventFieldMsgFormat eventFieldMsgFormat;

    @Builder
    public ExcelCardExport(String companyName, CompanyCardSchema companySchema, Project project, ProjectCardSchema schema,
                           List<Card> cards, List<CardField> fields, boolean exportOperations,
                           CardDao cardDao,
                           Function<List<Long>, List<Card>> findCardByIds,
                           Function<List<Long>, List<Plan>> findPlanByIds,
                           Function<List<Long>, List<StoryMapNode>> findStoryMapNodeByIds,
                           Function<List<Long>, Map<Long, List<String>>> findRelateCardKeys,
                           Function<Long, List<CardEvent>> findEventByCardId,
                           Function<Card, String> generateCardUrl) {
        this.companyName = companyName;
        this.companySchema = companySchema;
        this.project = project;
        this.schema = schema;
        this.cards = cards;
        this.fields = fields;
        this.exportOperations = exportOperations;
        this.cardDao = cardDao;
        this.findCardByIds = findCardByIds;
        this.findPlanByIds = findPlanByIds;
        this.findStoryMapNodeByIds = findStoryMapNodeByIds;
        this.findRelateCardKeys = findRelateCardKeys;
        this.findEventByCardId = findEventByCardId;
        this.generateCardUrl = generateCardUrl;
        this.eventFieldMsgFormat = new EventFieldMsgFormat(schema);
    }

    public void writeExportExcel(OutputStream excel) throws IOException {
        try {
            String[] fieldKeys = fields.stream().map(CardField::getKey).toArray(String[]::new);
            init(excel);
            ListUtils.partition(this.cards, 100).stream().forEach(cards -> {
                try {
                    Map<Long, Map<String, Object>> cardDetails = cardDao.findAsMap(cards.stream().map(Card::getId).collect(Collectors.toList()), fieldKeys);
                    cards.forEach(card -> writeRow(card, cardDetails.get(card.getId())));
                } catch (IOException e) {
                    log.error("Find es cards exception!", e);
                    throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
                }
            });
        } catch (Throwable e) {
            log.error("writeExportExcel exception!", e);
        } finally {
            if (null != this.excelWriter) {
                this.excelWriter.finish();
            }
        }
    }

    private void writeRow(Card card, Map<String, Object> cardDetail) {
        List<String> row = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            CardField field = fields.get(i);
            Object value = cardDetail.get(field.getKey());
            if (value == null) {
                row.add(null);
                continue;
            }
            try {
                switch (field.getKey()) {
                    case CardField.COMPANY_ID:
                        row.add(companyName);
                        break;
                    case CardField.PROJECT_ID:
                        row.add(project.getName());
                        break;
                    case CardField.PARENT_ID:
                        row.add(parentKeys.get(FieldUtil.toLong(cardDetail.get(field.getKey()))));
                        break;
                    case CardField.RELATED_CARD_IDS:
                        row.add(FieldUtil.toString(relatedCardKeys.get(card.getId())));
                        break;
                    case CardField.PLAN_ID:
                        row.add(planNames.get(FieldUtil.toLong(cardDetail.get(field.getKey()))));
                        break;
                    case CardField.STATUS:
                        row.add(statusNames.get(FieldUtil.toString(cardDetail.get(field.getKey()))));
                        break;
                    case CardField.TYPE:
                        row.add(typeNames.get(FieldUtil.toString(cardDetail.get(field.getKey()))));
                        break;
                    case CardField.STORY_MAP_NODE_ID:
                        row.add(storyMapNodeNames.get(FieldUtil.toLong(cardDetail.get(field.getKey()))));
                        break;
                    default:
                        if (FieldType.DATE == field.getType()) {
                            row.add(DATE_FORMAT.apply(cardDetail.get(field.getKey())));
                        } else if (FieldType.DATE_TIME == field.getType()) {
                            row.add(DATE_TIME_FORMAT.apply(cardDetail.get(field.getKey())));
                        } else {
                            row.add(getValidStringValue(field, cardDetail.get(field.getKey())));
                        }
                }
            } catch (Exception e) {
                log.error("convert field to string value exception!", e);
                row.add(null);
            }
        }
        if (exportOperations) {
            List<CardEvent> events = findEventByCardId.apply(card.getId());
            VelocityContext context = new VelocityContext();
            context.put("events", events);
            context.put("dateFormat", DATE_TIME_FORMAT);
            context.put("fieldFormat", this.eventFieldMsgFormat);
            try {
                row.add(VelocityTemplate.render(context, "/vm/export-card-events.tpl"));
            } catch (Exception e) {
                row.add(null);
                log.error("export render events exception!", e);
            }
        }
        this.excelWriter.write(Arrays.asList(row), writeSheet);
    }

    private String getValidStringValue(CardField field, Object value) {
        switch (field.getType()) {
            case SELECT:
            case RADIO:
                return FieldUtil.getValidNameInOptions(value, field);
            case CHECK_BOX:
                return FieldUtil.toString(FieldUtil.getValidNamesInOptions(value, field));
            default:
                return FieldUtil.toString(value);
        }
    }

    private static final Function<Object, String> DATE_FORMAT = date -> {
        try {
            if (date instanceof String && NumberUtils.isNumber((String)date)){
                date = Long.parseLong((String)date);
            }
            return new DateTime(date).toString("yyyy-MM-dd");
        } catch (Exception e) {
            log.error("format date exception:[%s]!", e.getMessage());
            return FieldUtil.toString(date);
        }
    };

    private static final Function<Object, String> DATE_TIME_FORMAT = date -> {
        try {
            if (date instanceof String && NumberUtils.isNumber((String)date)){
                date = Long.parseLong((String)date);
            }
            return new DateTime(date).toString("yyyy-MM-dd HH:mm:ss");
        } catch (Exception e) {
            log.error("format date exception:[%s]!", e.getMessage());
            return FieldUtil.toString(date);
        }
    };

    private static final List<String> LINK_FIELD_KEYS = Arrays.asList(CardField.SEQ_NUM, CardField.TITLE);

    private void init(OutputStream excel) {
        List<String> fieldNames = new ArrayList<>();
        List<Integer> linkFieldIndexes = new ArrayList<>();
        for (int i = 0; i < fields.size(); i++) {
            CardField field = fields.get(i);
            switch (field.getKey()) {
                case CardField.PARENT_ID:
                    List<Long> parentIds = cards.stream()
                            .map(Card::getParentId)
                            .filter(id -> id > 0)
                            .collect(Collectors.toList());
                    parentKeys = findCardByIds.apply(parentIds).stream().collect(Collectors.toMap(Card::getId,
                            c -> String.format("%s-%s", c.getProjectKey(), c.getSeqNum())));
                    break;
                case CardField.PLAN_ID:
                    List<Long> planIds = cards.stream()
                            .map(Card::getPlanId)
                            .filter(id -> id > 0)
                            .collect(Collectors.toList());
                    planNames = findPlanByIds.apply(planIds).stream()
                            .collect(Collectors.toMap(Plan::getId, Plan::getName));
                    break;
                case CardField.STORY_MAP_NODE_ID:
                    List<Long> storyMapNodeIds = cards.stream()
                            .map(Card::getStoryMapNodeId)
                            .filter(id -> id > 0)
                            .collect(Collectors.toList());
                    storyMapNodeNames = findStoryMapNodeByIds.apply(storyMapNodeIds).stream()
                            .collect(Collectors.toMap(StoryMapNode::getId, StoryMapNode::getName));
                    break;
                case CardField.RELATED_CARD_IDS:
                    relatedCardKeys = findRelateCardKeys.apply(cards.stream().map(Card::getId).collect(Collectors.toList()));
                default:
                    if (LINK_FIELD_KEYS.contains(field.getKey())) {
                        linkFieldIndexes.add(i);
                    }
            }
            fieldNames.add(field.getName());
        }
        this.statusNames = schema.getStatuses().stream().collect(Collectors.toMap(s -> s.getKey(), s -> s.getName()));
        this.typeNames = companySchema.getTypes().stream().collect(Collectors.toMap(t -> t.getKey(), t -> t.getName()));
        this.excelWriter = EasyExcel.write(excel).registerWriteHandler(excelStyle(linkFieldIndexes)).build();
        this.writeSheet = EasyExcel.writerSheet("sheet1").build();
        if (exportOperations) {
            fieldNames.add("操作记录");
        }
        this.excelWriter.write(Arrays.asList(fieldNames), writeSheet);
    }

    private CellWriteHandler excelStyle(List<Integer> linkFieldIndexes) {
        return new AbstractCellWriteHandler() {
            @Override
            public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                         List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                         Integer relativeRowIndex, Boolean isHead) {
                Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
                if (cell.getRowIndex() == 0) {
                    if (!exportOperations || cell.getColumnIndex() != fields.size()) {
                        CellStyle style = workbook.createCellStyle();
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                        cell.setCellStyle(style);
                    }
                } else if (linkFieldIndexes.contains(cell.getColumnIndex())) {
                    Card card = cards.get(cell.getRowIndex() - 1);
                    Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(generateCardUrl.apply(card));
                    cell.setHyperlink(link);
                    CellStyle style = workbook.createCellStyle();
                    Font font = workbook.createFont();
                    font.setColor(IndexedColors.BLUE.getIndex());
                    font.setUnderline(Font.U_SINGLE);
                    style.setFont(font);
                    cell.setCellStyle(style);
                }
            }
        };
    }

    public static class EventFieldMsgFormat {
        private Map<String, FieldType> fieldMap;
        private Map<String, CardStatus> statusMap;

        public EventFieldMsgFormat(ProjectCardSchema schema) {
            fieldMap = schema.getFields().stream().collect(Collectors.toMap(CardField::getKey, CardField::getType));
            statusMap = schema.getStatuses().stream().collect(Collectors.toMap(CardStatus::getKey, Function.identity(), (cardStatus, cardStatus2) -> cardStatus2));
        }

        public String format(String fieldKey, String msg) {
            if (StringUtils.isEmpty(msg)) {
                return msg;
            }
            if (CardField.STATUS.equals(fieldKey)) {
                CardStatus cardStatus = statusMap.get(msg);
                return cardStatus == null ? "" : cardStatus.getName();
            }
            FieldType fieldType = fieldMap.get(fieldKey);
            switch (fieldType) {
                case DATE:
                    return DATE_FORMAT.apply(NumberUtils.toLong(msg));
                case DATE_TIME:
                    return DATE_TIME_FORMAT.apply(NumberUtils.toLong(msg));
                default:
                    return msg;
            }
        }
    }
}
