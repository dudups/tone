package com.ezone.ezproject.modules.alarm.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.dal.entity.CardAlarmNoticePlan;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.modules.alarm.bean.AlarmDateConfig;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.PlanAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.EventMsg;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.event.events.CardCreateEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsCreateEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class AlarmNoticeService {
    private AlarmConfigQueryService alarmConfigQueryService;
    private ProjectSchemaQueryService schemaQueryService;
    private CardSearchService cardSearchService;
    private CardAlarmNoticePlanService cardAlarmNoticePlanService;

    /**
     * 当删除规则集时，处理在指定周期内需要发送的消息的卡片。
     *
     * @param projectAlarm 新增的项目预警设置
     */
    public void deleteNoticePlans(@NotNull ProjectAlarmExt projectAlarm) {
        if (!(projectAlarm.getAlarmItem() instanceof CardAlarmItem)) {
            return;
        }
        cardAlarmNoticePlanService.deleteByAlarmId(projectAlarm.getId());
    }

    @Async
    @AfterCommit
    public void asyncDeleteNoticePlans(@NotNull ProjectAlarmExt projectAlarm) {
        deleteNoticePlans(projectAlarm);
    }

    /**
     * 当修改规则集时，处理在指定周期内需要发送的消息的卡片。
     *
     * @param projectAlarm      新增的项目预警设置
     * @param noticeStart       通知计划的开始时间，为空表示之后所有时间。
     * @param planCycleCrossDay 通知计划跨越的周期，如果noticeStart为空，将忽略该参数。
     */
    @Async
    @AfterCommit
    public void updateNoticePlans(@NotNull ProjectAlarmExt projectAlarm, Date noticeStart, Integer planCycleCrossDay) throws IOException {
        if (!(projectAlarm.getAlarmItem() instanceof CardAlarmItem)) {
            return;
        }
        //预警设置变化，删除旧的，添加新的。
        deleteNoticePlans(projectAlarm);
        genNoticePlans(projectAlarm, noticeStart, planCycleCrossDay);
    }

    /**
     * 当新增规则集时，处理在指定周期内需要发送的消息的卡片。
     *
     * @param projectAlarm      新增的项目预警设置
     * @param noticeStart       通知计划的开始时间，为空表示之后所有时间。为空表示不管多久以后，都生成执行计划。
     * @param planCycleCrossDay 通知计划跨越的周期，如果noticeStart为空，将忽略该参数。
     */
    public void genNoticePlans(@NotNull ProjectAlarmExt projectAlarm, Date noticeStart, Integer planCycleCrossDay) throws IOException {
        if (!(projectAlarm.getAlarmItem() instanceof CardAlarmItem)) {
            return;
        }
        genAlarmNotices(projectAlarm.getProjectId(), noticeStart, planCycleCrossDay, Collections.singletonList(projectAlarm));
    }

    @AfterCommit
    @Async
    public void genAlarmNoticePlan(CardCreateEvent event) {
        genAlarmNoticePlans(event.getCard().getProjectId(), event.getCard().getId(), event.getCardDetail());
    }


    @AfterCommit
    @Async
    public void genAlarmNoticePlan(CardsCreateEvent event) {
        genNoticePlans(event.getProjectId(), event.getCardDetails());
    }

    @AfterCommit
    @Async
    public void updateAlarmNoticePlan(CardUpdateEvent event) {
        EventMsg eventMsg = event.getCardEvent().getEventMsg();
        if (eventMsg instanceof UpdateEventMsg) {
            Map<String, Object> cardDetail = event.getCardEvent().getCardDetail();
            cardDetail.put(CardField.LAST_MODIFY_TIME, event.getCardEvent().getDate().getTime());
            updateNoticePlans(event.getCard().getProjectId(), event.getCard().getId(), cardDetail);
        }
    }

    @AfterCommit
    @Async
    public void updateAlarmNoticePlan(CardsUpdateEvent event) {
        Map<Long, Map<String, Object>> cardDetails = event.getCardEvents().stream().filter(cardEvent -> {
            EventMsg eventMsg = cardEvent.getEventMsg();
            return eventMsg instanceof UpdateEventMsg;
        }).collect(Collectors.toMap(CardEvent::getCardId, CardEvent::getCardDetail));
        updateNoticePlans(event.getProject().getId(), cardDetails);
    }

    /**
     * 当创建卡片时，处理在指定周期内需要发送的消息的卡片预警。
     *
     * @param projectId
     * @param cardId
     * @param cardDetail
     * @throws IOException
     */
    private void genAlarmNoticePlans(Long projectId, Long cardId, Map<String, Object> cardDetail) {
        List<ProjectAlarmExt> projectAlarms = alarmConfigQueryService.getProjectAlarms(projectId);
        ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
        Map<Long, Map<String, Object>> cardMaps = new HashMap<>();
        cardMaps.put(cardId, cardDetail);
        List<CardAlarmNoticePlan> cardAlarmNoticePlans = calcNoticePlans(projectId, projectAlarms, cardMaps, projectCardSchema);
        cardAlarmNoticePlanService.save(cardAlarmNoticePlans);
    }

    /**
     * 当创建卡片时，处理在指定周期内需要发送的消息的卡片预警。
     *
     * @param projectId
     * @param cardDetails
     * @throws IOException
     */
    private void genNoticePlans(Long projectId, Map<Long, Map<String, Object>> cardDetails) {
        List<ProjectAlarmExt> projectAlarms = alarmConfigQueryService.getProjectAlarms(projectId);
        ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
        List<CardAlarmNoticePlan> cardAlarmNoticePlans = calcNoticePlans(projectId, projectAlarms, cardDetails, projectCardSchema);
        cardAlarmNoticePlanService.save(cardAlarmNoticePlans);
    }

    /**
     * 当卡片更新时，处理在指定周期内需要发送的消息的卡片预警。
     *
     * @param projectId
     * @param cardId
     * @param cardDetail
     * @throws IOException
     */
    private void updateNoticePlans(Long projectId, Long cardId, Map<String, Object> cardDetail) {
        List<ProjectAlarmExt> projectAlarms = alarmConfigQueryService.getProjectAlarms(projectId);
        cardAlarmNoticePlanService.deleteByCardIds(Collections.singletonList(cardId));
        ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
        Map<Long, Map<String, Object>> cardMaps = new HashMap<>();
        cardMaps.put(cardId, cardDetail);
        List<CardAlarmNoticePlan> cardAlarmNoticePlans = calcNoticePlans(projectId, projectAlarms, cardMaps, projectCardSchema);
        cardAlarmNoticePlanService.save(cardAlarmNoticePlans);
    }

    private void updateNoticePlans(Long projectId, Map<Long, Map<String, Object>> cardDetails) {
        if (MapUtils.isEmpty(cardDetails)) {
            return;
        }
        List<ProjectAlarmExt> projectAlarms = alarmConfigQueryService.getProjectAlarms(projectId);
        ArrayList<Long> cardIds = new ArrayList<>(cardDetails.keySet());
        cardAlarmNoticePlanService.deleteByCardIds(cardIds);
        ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
        List<CardAlarmNoticePlan> cardAlarmNoticePlans = calcNoticePlans(projectId, projectAlarms, cardDetails, projectCardSchema);
        cardAlarmNoticePlanService.save(cardAlarmNoticePlans);
    }

    /**
     * 当发送计划中发送时间与之前保存的相同时，采取的策略是新添加，不修改原先的发送计划。在发送时合并。
     *
     * @param projectId
     * @param noticeStart
     * @param planCycleCrossDay
     * @param projectAlarms
     * @throws IOException
     */
    protected void genAlarmNotices(Long projectId, Date noticeStart, Integer planCycleCrossDay, List<ProjectAlarmExt> projectAlarms) throws IOException {
        List<String> requiredCardKey = projectAlarms.stream().filter(projectAlarm -> projectAlarm.getAlarmItem() instanceof CardAlarmItem).map(projectAlarm -> projectAlarm.getAlarmItem().getDateFieldKey()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(requiredCardKey)) {
            return;
        }
        requiredCardKey.add(CardField.TYPE);
        ProjectCardSchema projectCardSchema = schemaQueryService.getProjectCardSchema(projectId);
        List<Query> queries = Arrays.asList(Eq.builder().field(CardField.PROJECT_ID).value(projectId.toString()).build());
        SearchEsRequest searchCardRequest = SearchEsRequest.builder().queries(queries).fields(requiredCardKey.toArray(new String[0])).build();
        int pageSize = 1000;
        int pageNumber = 1;
        TotalBean<CardBean> totalBean = cardSearchService.search(projectId, searchCardRequest, false, pageNumber, pageSize);
        calcNoticePlansAndSave(projectId, noticeStart, planCycleCrossDay, projectAlarms, projectCardSchema, totalBean);
        while (totalBean.getTotal() > pageSize * pageNumber) {
            pageNumber++;
            totalBean = cardSearchService.search(projectId, searchCardRequest, false, pageNumber, pageSize);
            calcNoticePlansAndSave(projectId, noticeStart, planCycleCrossDay, projectAlarms, projectCardSchema, totalBean);
        }
    }


    private void calcNoticePlansAndSave(Long projectId, Date noticeStart, Integer planCycleCrossDay, List<ProjectAlarmExt> projectAlarms, ProjectCardSchema projectCardSchema, TotalBean<CardBean> totalBean) {
        List<CardBean> cardBeans = totalBean.getList();
        if (CollectionUtils.isNotEmpty(cardBeans)) {
            Map<Long, Map<String, Object>> cardMaps = cardBeans.stream().collect(Collectors.toMap(CardBean::getId, CardBean::getCard));
            List<CardAlarmNoticePlan> cardAlarmNoticePlans = calcNoticePlans(projectId, projectAlarms, cardMaps, projectCardSchema, noticeStart, planCycleCrossDay);
            cardAlarmNoticePlanService.save(cardAlarmNoticePlans);
        }
    }

    public boolean validNoticePlan(ProjectAlarmExt projectAlarm, Long cardId, Map<String, Object> cardDetail, ProjectCardSchema schema, CardAlarmNoticePlan noticePlan) {
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        cardDetails.put(cardId, cardDetail);
        Map<String, CardType> cardTypes = schema.getTypes().stream().collect(Collectors.toMap(CardType::getKey, Function.identity()));
        Date now = new Date();
        String type = FieldUtil.getType(cardDetail);
        if (type == null) {
            return false;
        }
        CardType cardType = cardTypes.get(type);
        if (cardType == null) {
            return false;
        }
        CardType.FieldConf fieldConf = cardType.findFieldConf(projectAlarm.getAlarmItem().getDateFieldKey());
        if (fieldConf == null || !fieldConf.isEnable()) {
            return false;
        }
        return noticePlan.getTimestampMinute() <= now.getTime() / 1000 / 60 - 1;
    }

    /**
     * 计算出卡片预警计划（时间范围：从通知的计划开始时间开始，加上通知计划跨越的周期结束的时间）
     *
     * @param projectAlarms 预警配置
     * @param cardDetails   卡片明细, 必须包含卡片类型、可以只包含时间类型的字段值及
     * @param schema        项目schema定义
     * @return
     */
    protected List<CardAlarmNoticePlan> calcNoticePlans(Long projectId, List<ProjectAlarmExt> projectAlarms, Map<Long, Map<String, Object>> cardDetails, ProjectCardSchema schema) {
        return calcNoticePlans(projectId, projectAlarms, cardDetails, schema, null, null);
    }

    /**
     * 计算出卡片预警计划（时间范围：从通知的计划开始时间开始，加上通知计划跨越的周期结束的时间）
     *
     * @param projectAlarms     预警配置
     * @param cardDetails       卡片明细, 必须包含卡片类型、可以只包含时间类型的字段值及
     * @param schema            项目schema定义
     * @param noticeStart       生成通知计划的开始时间，为空表示之后所有时间。为空表示不管多久以后，都生成执行计划。
     * @param planCycleCrossDay 通知计划跨越的周期，如果noticeStart为空，将忽略该参数。
     * @return
     */
    protected List<CardAlarmNoticePlan> calcNoticePlans(Long projectId, List<ProjectAlarmExt> projectAlarms, Map<Long, Map<String, Object>> cardDetails, ProjectCardSchema schema, Date noticeStart, Integer planCycleCrossDay) {
        if (CollectionUtils.isEmpty(projectAlarms) || MapUtils.isEmpty(cardDetails)) {
            return Collections.emptyList();
        }
        List<ProjectAlarmExt> cardProjectAlarms = projectAlarms.stream().filter(ProjectAlarmExt::getActive).filter(alarmConfig -> alarmConfig.getProjectId().equals(projectId) && alarmConfig.getAlarmItem() instanceof CardAlarmItem).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cardProjectAlarms)) {
            return Collections.emptyList();
        }
        Map<String, CardType> cardTypes = schema.getTypes().stream().collect(Collectors.toMap(CardType::getKey, Function.identity()));
        Date now = new Date();
        List<CardAlarmNoticePlan> noticePlans = new ArrayList<>();
        cardDetails.forEach((cardId, cardDetail) -> {
            String type = FieldUtil.getType(cardDetail);
            if (type == null) {
                return;
            }
            CardType cardType = cardTypes.get(type);
            if (cardType == null) {
                return;
            }
            for (ProjectAlarmExt projectAlarm : cardProjectAlarms) {
                Long noticeTimeMinute = getAlarmNoticeTimeMinute(schema, cardDetail, cardType, projectAlarm);
                if (noticeTimeMinute != null && isNeedGenPlan(noticeStart, planCycleCrossDay, noticeTimeMinute, now)) {
                    CardAlarmNoticePlan noticePlan = CardAlarmNoticePlan.builder().id(IdUtil.generateId()).projectId(projectId).cardId(cardId).alarmId(projectAlarm.getId()).timestampMinute(noticeTimeMinute.intValue()).sendFlag(0).build();
                    noticePlans.add(noticePlan);
                }
            }
        });
        return noticePlans;
    }


    /**
     * 检查发送时间，是否需要通知<br>
     * 如果未指定noticeStart，刚返回true;如果noticeTime为空，返回false;
     *
     * @param noticeStart
     * @param planCycleCrossDay 生成通知计划的开始时间，为空表示之后所有时间。为空表示不管多久以后，都生成执行计划。
     * @param noticeTimeMinute
     * @return
     */
    private boolean isNeedGenPlan(Date noticeStart, Integer planCycleCrossDay, Long noticeTimeMinute, Date now) {
        if (noticeTimeMinute == null) return false;
        if (noticeStart != null) {
            Date endDate = DateUtils.addDays(noticeStart, planCycleCrossDay);
            return noticeTimeMinute >= now.getTime() / 1000 / 60 - 1 && noticeTimeMinute >= noticeStart.getTime() && noticeTimeMinute < endDate.getTime();
        } else {
            return noticeTimeMinute >= now.getTime() / 1000 / 60 - 1;
        }
    }

    @Nullable
    private Long getAlarmNoticeTimeMinute(ProjectCardSchema schema, Map<String, Object> cardDetail, CardType cardType, ProjectAlarmExt projectAlarm) {
        AlarmItem alarmItem = projectAlarm.getAlarmItem();
        String alarmCardKey = alarmItem.getDateFieldKey();
        Long fieldTime;
        if (CardField.LAST_MODIFY_TIME.equals(alarmCardKey)) {
            AlarmDateConfig alarmDateConfig = alarmItem.getAlarmDateConfig();
            return (FieldUtil.getLastModifyTime(cardDetail) + alarmDateConfig.numberOfMillisecond()) / 60 / 1000;
        } else {
            CardType.FieldConf fieldConf = cardType.findFieldConf(alarmCardKey);
            if (fieldConf == null) {
                return null;
            }
            if (!fieldConf.isEnable()) {
                return null;
            }
            fieldTime = FieldUtil.getDateTypeFieldValue(cardDetail, alarmCardKey);
            //未设置时间的，忽略预警配置。
            if (fieldTime == 0 || fieldTime == null) {
                return null;
            }
            CardField cardField = schema.findCardField(alarmCardKey);
            if (cardField == null) {
                //字段被删除或错误，忽略预警配置。
                return null;
            }
            AlarmDateConfig alarmDateConfig = alarmItem.getAlarmDateConfig();
            long noticeTime = (fieldTime + alarmDateConfig.numberOfMillisecond()) / 60 / 1000;
            //计划结束日期填与的是当日凌晨，实际使用时要到当晚24时才算结束。用户自定义字段也以结束为标识
            if (CardField.END_DATE.equals(alarmCardKey) || (Source.CUSTOM.equals(cardField.getSource()) && FieldType.DATE.equals(cardField.getType()))) {
                noticeTime = noticeTime + 24 * 60;
            }
            return noticeTime;
        }
    }

    @Nullable
    public Integer getAlarmNoticeTimestampMinute(Plan plan, PlanAlarmItem planAlarmItem) {
        String alarmPlanKey = planAlarmItem.getDateFieldKey();
        Long fieldTime;
        if (PlanAlarmItem.FIELD_KEY_START_TIME.equals(alarmPlanKey)) {
            fieldTime = plan.getStartTime().getTime();
        } else if (PlanAlarmItem.FIELD_KEY_END_TIME.equals(alarmPlanKey)) {
            fieldTime = plan.getEndTime().getTime() + 86400000;
        } else {
            return null;
        }
        AlarmDateConfig alarmDateConfig = planAlarmItem.getAlarmDateConfig();
        long noticeTime = fieldTime + alarmDateConfig.numberOfMillisecond();
        return (int) (noticeTime / 1000 / 60);
    }

    public @Nullable
    Integer getAlarmNoticeTimestampMinute(ProjectExt projectExt, ProjectAlarmItem projectAlarmItem) {
        String alarmPlanKey = projectAlarmItem.getDateFieldKey();
        Long fieldTime;
        if (ProjectAlarmItem.FIELD_KEY_START_TIME.equals(alarmPlanKey) && projectExt.getStartTime() != null) {
            fieldTime = projectExt.getStartTime().getTime();
        } else if (ProjectAlarmItem.FIELD_KEY_END_TIME.equals(alarmPlanKey) && projectExt.getEndTime() != null) {
            fieldTime = projectExt.getEndTime().getTime() + 86400000;
        } else {
            return null;
        }
        AlarmDateConfig alarmDateConfig = projectAlarmItem.getAlarmDateConfig();
        long noticeTime = fieldTime + alarmDateConfig.numberOfMillisecond();
        return (int) (noticeTime / 1000 / 60);
    }
}
