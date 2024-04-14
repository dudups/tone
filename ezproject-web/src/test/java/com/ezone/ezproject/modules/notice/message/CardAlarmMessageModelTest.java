package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.common.JsonUtil;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.alarm.bean.AlarmDateConfig;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.NoticeFieldUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeSpecUsersConfig;
import com.ezone.ezproject.modules.common.EndpointHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectCardSchema.class, EndpointHelper.class})
class CardAlarmMessageModelTest {

    @Mock
    private EndpointHelper endpointHelper;
    private Long companyId = 1L;
    private Project project;
    private ProjectAlarmExt cardAlarm;
    private ProjectAlarmExt planAlarm;
    @Mock
    private ProjectCardSchema projectCardSchema;

    @BeforeEach
    public void before() throws Exception {
        endpointHelper = PowerMockito.mock(EndpointHelper.class);
        projectCardSchema = PowerMockito.mock(ProjectCardSchema.class);
        Mockito.when(endpointHelper.cardDetailUrl(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn("http://localhost/card/222");
        project = Project.builder().id(1L).key("myProject").companyId(companyId).name("我的项目").build();
        Mockito.when(endpointHelper.cardDetailPath(Mockito.anyString(), Mockito.anyLong())).thenReturn("/ezProject/cf8/33'");
        CardField cardField = new CardField();
        cardField.setName("计划开始时间");
        Mockito.when(projectCardSchema.findCardField(Mockito.any())).thenReturn(cardField);

        CardAlarmItem cardItem = new CardAlarmItem();
        cardItem.setDateFieldKey("endTime");
        cardItem.setName("卡片结束提前预警");
        cardItem.setWarningUsers(Arrays.asList(
                NoticeFieldUsersConfig.builder().userFields(Arrays.asList("create")).build(),
                NoticeSpecUsersConfig.builder().users(Arrays.asList(new GroupUser("ycf", GroupUserType.USER), new GroupUser("zhangSan", GroupUserType.USER))).build()));
        CardAlarmItem alarmConfigItem = new CardAlarmItem();
        AlarmDateConfig alarmDateConfig = new AlarmDateConfig();
        alarmDateConfig.setDateField(AlarmDateConfig.DateField.DAY);
        alarmDateConfig.setNumber(10);
        alarmConfigItem.setAlarmDateConfig(alarmDateConfig);
        cardItem.setAlarmDateConfig(alarmDateConfig);

        Long projectId = 1L;
        cardAlarm = ProjectAlarmExt.builder().projectId(projectId).alarmItem(cardItem).build();
    }

    @Test
    void getFeiShuContent() {
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        Map<String, Object> cardDetail = new HashMap<>();
        cardDetails.put(1L, cardDetail);
        cardDetail.put(CardField.PROJECT_ID, 222L);
        cardDetail.put(CardField.TITLE, "卡片标题");
        cardDetail.put(CardField.SEQ_NUM, 1L);
        cardDetail.put(CardField.COMPANY_ID, companyId);
        CardAlarmMessageModel messageModel = CardAlarmMessageModel.builder()
                .cardDetail(cardDetail)
                .project(project)
                .alarmItem(cardAlarm.getAlarmItem())
                .projectCardSchema(projectCardSchema)
                .endpointHelper(endpointHelper)
                .build();
        String feiShuContent = messageModel.getFeiShuContent();
        System.out.println(feiShuContent);

        boolean isJson = JsonUtil.isValidJSON(feiShuContent);
        assertTrue(isJson);
    }


    @Test
    void getMailContent() {
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        Map<String, Object> cardDetail = new HashMap<>();
        cardDetails.put(1L, cardDetail);
        cardDetail.put(CardField.PROJECT_ID, 222L);
        cardDetail.put(CardField.TITLE, "卡片标题");
        cardDetail.put(CardField.SEQ_NUM, 1L);
        cardDetail.put(CardField.COMPANY_ID, companyId);
        CardAlarmMessageModel messageModel = CardAlarmMessageModel.builder()
                .cardDetail(cardDetail)
                .project(project)
                .alarmItem(cardAlarm.getAlarmItem())
                .projectCardSchema(projectCardSchema)
                .endpointHelper(endpointHelper)
                .build();
        String feiShuContent = messageModel.getEmailContent();
        System.out.println(feiShuContent);
    }

    @Test
    void getSystemContent() {
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        Map<String, Object> cardDetail = new HashMap<>();
        cardDetails.put(1L, cardDetail);
        cardDetail.put(CardField.PROJECT_ID, 222L);
        cardDetail.put(CardField.TITLE, "卡片标题");
        cardDetail.put(CardField.SEQ_NUM, 1L);
        cardDetail.put(CardField.COMPANY_ID, companyId);
        CardAlarmMessageModel messageModel = CardAlarmMessageModel.builder()
                .cardDetail(cardDetail)
                .project(project)
                .alarmItem(cardAlarm.getAlarmItem())
                .projectCardSchema(projectCardSchema)
                .endpointHelper(endpointHelper)
                .build();
        String feiShuContent = messageModel.getContent();
        System.out.println(feiShuContent);
    }

}