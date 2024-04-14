package com.ezone.ezproject.modules.alarm.service;

import com.ezone.ezproject.dal.entity.CardAlarmNoticePlan;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.alarm.bean.AlarmDateConfig;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author cf
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AlarmConfigCmdService.class, ProjectSchemaQueryService.class, CardSearchService.class, CardAlarmNoticePlanService.class})
class AlarmNoticeServiceTest {
    private AlarmConfigQueryService alarmService;
    private ProjectSchemaQueryService schemaQueryService;
    private CardSearchService cardSearchService;
    private CardAlarmNoticePlanService cardAlarmNoticePlanService;


    @Test
    void calcNoticePlans() {
        alarmService = PowerMockito.mock(AlarmConfigQueryService.class);
        schemaQueryService = PowerMockito.mock(ProjectSchemaQueryService.class);
        cardSearchService = PowerMockito.mock(CardSearchService.class);
        cardAlarmNoticePlanService = PowerMockito.mock(CardAlarmNoticePlanService.class);

        AlarmNoticeService service = new AlarmNoticeService(alarmService, schemaQueryService, cardSearchService, cardAlarmNoticePlanService);
        Long projectId = 1L;
        Long cardId = 1L;
        String cardType = "task";

        Map<String, Object> cardDetail = new HashMap<>();
        ProjectCardSchema projectCardSchema = new ProjectCardSchema();
        projectCardSchema.setTypes(Arrays.asList(CardType.builder().key(cardType).enable(true)
                .field(CardType.FieldConf.builder().key(CardField.START_DATE).enable(true).build()).build()));
        projectCardSchema.setFields(Arrays.asList(CardField.builder().key(CardField.START_DATE).build()));
        Map<Long, Map<String, Object>> cardMaps = new HashMap<>();
        cardMaps.put(cardId, cardDetail);
        CardAlarmItem item = new CardAlarmItem();
        List<ProjectAlarmExt> projectAlarms = Arrays.asList(
                ProjectAlarmExt.builder().projectId(projectId)
                        .alarmItem(item)
                        .build()
        );
        item.setDateFieldKey(CardField.START_DATE);
        item.setName("计划开始前提醒");
        int alarmDayBefore = -1;
        AlarmDateConfig dateConfig = AlarmDateConfig.builder().dateField(AlarmDateConfig.DateField.DAY).number(alarmDayBefore).build();
        long millisecond = dateConfig.numberOfMillisecond();
        assertEquals(-1 * 60 * 60 * 24 * 1000, millisecond);
        item.setAlarmDateConfig(dateConfig);
        Date now = new Date();
        Date startDay = DateUtils.addDays(now, 7);
        cardDetail.put(CardField.START_DATE, startDay.getTime());
        cardDetail.put(CardField.TYPE, cardType);
        List<CardAlarmNoticePlan> cardAlarmNoticePlans = service.calcNoticePlans(projectId, projectAlarms, cardMaps, projectCardSchema);
        assertTrue(CollectionUtils.isNotEmpty(cardAlarmNoticePlans));
        int actualNoticeTime = cardAlarmNoticePlans.get(0).getTimestampMinute();
        long time = DateUtils.addDays(startDay, alarmDayBefore).getTime();
        int expected = (int) (time / 1000 / 60);
        assertEquals(expected, actualNoticeTime);
    }
}