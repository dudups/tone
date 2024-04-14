package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.common.JsonUtil;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.modules.alarm.bean.AlarmDateConfig;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.NoticeFieldUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeSpecUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.PlanAlarmItem;
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
@PrepareForTest({EndpointHelper.class})
class PlanArmMessageModelTest {

    @Mock
    private EndpointHelper endpointHelper;
    private Long companyId = 1L;
    private Project project;
    private ProjectAlarmExt planAlarm;

    @BeforeEach
    public void before() throws Exception {
        endpointHelper = PowerMockito.mock(EndpointHelper.class);
        Mockito.when(endpointHelper.cardDetailUrl(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn("http://localhost/card/222");
        project = Project.builder().id(1L).key("myProject").companyId(companyId).name("我的项目").build();
        Mockito.when(endpointHelper.userHomeUrl(Mockito.anyLong(), Mockito.any())).thenReturn("http://localhost/user/zhangsan");
        CardField cardField = new CardField();
        cardField.setName("计划开始时间");
        PlanAlarmItem planItem = new PlanAlarmItem();
        planItem.setDateFieldKey(PlanAlarmItem.FIELD_KEY_END_TIME);
        planItem.setName("计划结束提前预警");
        planItem.setWarningUsers(Arrays.asList(
                NoticeFieldUsersConfig.builder().userFields(Arrays.asList("create")).build(),
                NoticeSpecUsersConfig.builder().users(Arrays.asList(new GroupUser("ycf", GroupUserType.USER), new GroupUser("zhangSan", GroupUserType.USER))).build()));
        CardAlarmItem alarmConfigItem = new CardAlarmItem();
        AlarmDateConfig alarmDateConfig = new AlarmDateConfig();
        alarmDateConfig.setDateField(AlarmDateConfig.DateField.DAY);
        alarmDateConfig.setNumber(10);
        alarmConfigItem.setAlarmDateConfig(alarmDateConfig);
        planItem.setAlarmDateConfig(alarmDateConfig);

        Long projectId = 1L;
        planAlarm = ProjectAlarmExt.builder().projectId(projectId).alarmItem(planItem).build();
    }

    @Test
    void getFeiShuContent() {
        Plan plan = Plan.builder().name("五月第一周").build();
        PlanAlarmMessageModel messageModel = PlanAlarmMessageModel.builder()
                .plan(plan)
                .project(project)
                .alarmItem(planAlarm.getAlarmItem())
                .endpointHelper(endpointHelper)
                .build();
        String feiShuContent = messageModel.getFeiShuContent();
        System.out.println(feiShuContent);

        boolean isJson = JsonUtil.isValidJSON(feiShuContent);
        assertTrue(isJson);
    }


    @Test
    void getMailContent() {
        Plan plan = Plan.builder().name("五月第一周").build();
        PlanAlarmMessageModel messageModel = PlanAlarmMessageModel.builder()
                .plan(plan)
                .project(project)
                .alarmItem(planAlarm.getAlarmItem())
                .endpointHelper(endpointHelper)
                .build();
        String feiShuContent = messageModel.getEmailContent();
        System.out.println(feiShuContent);
    }

    @Test
    void getSystemContent() {
        Plan plan = Plan.builder().name("五月第一周").build();
        PlanAlarmMessageModel messageModel = PlanAlarmMessageModel.builder()
                .plan(plan)
                .project(project)
                .alarmItem(planAlarm.getAlarmItem())
                .endpointHelper(endpointHelper)
                .build();
        String feiShuContent = messageModel.getContent();
        System.out.println(feiShuContent);
    }

}