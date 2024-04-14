package com.ezone.ezproject.es.dao;

import com.ezone.ezbase.iam.bean.GroupUser;
import com.ezone.ezbase.iam.bean.enums.GroupUserType;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.modules.alarm.bean.AlarmDateConfig;
import com.ezone.ezproject.modules.alarm.bean.CardAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.NoticeFieldUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeSpecUsersConfig;
import com.ezone.ezproject.modules.alarm.bean.NoticeUserConfig;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class AlarmItemDaoTest {

    @Test
    void ProjectAlarmToJson() throws JsonProcessingException {
        ProjectAlarmItem projectItem = new ProjectAlarmItem();
        projectItem.setDateFieldKey("endTime");
        projectItem.setName("项目结束提前预警");
        NoticeFieldUsersConfig projectFieldUser = NoticeFieldUsersConfig.builder().userFields(Arrays.asList("create")).build();
        List<NoticeUserConfig> projectWarningUsers = Arrays.asList(
                projectFieldUser,
                NoticeSpecUsersConfig.builder().users(Arrays.asList(new GroupUser("ycf", GroupUserType.USER), new GroupUser("zhangSan", GroupUserType.USER))).build());
        projectItem.setWarningUsers(projectWarningUsers);

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
        ProjectAlarmExt projectAlarm = ProjectAlarmExt.builder().projectId(projectId).alarmItem(projectItem).build();
        String projectItemString = ProjectMenuDao.JSON_MAPPER.writeValueAsString(projectAlarm);
        System.out.println(projectItemString);
        ProjectAlarmExt alarm = ProjectMenuDao.JSON_MAPPER.readValue(projectItemString, ProjectAlarmExt.class);

        Assert.assertEquals(projectId, alarm.getProjectId());
        Assert.assertEquals("项目结束提前预警", alarm.getAlarmItem().getName());
        Assert.assertEquals("endTime", alarm.getAlarmItem().getDateFieldKey());
        NoticeUserConfig noticeUserConfig = alarm.getAlarmItem().getWarningUsers().get(0);
        Assert.assertTrue(noticeUserConfig instanceof NoticeFieldUsersConfig);
        NoticeFieldUsersConfig fieldUserConfig = (NoticeFieldUsersConfig) noticeUserConfig;
        Assert.assertTrue(CollectionUtils.isEqualCollection(fieldUserConfig.getUserFields(), projectFieldUser.getUserFields()));


        ProjectAlarmExt cardAlarm = ProjectAlarmExt.builder().projectId(projectId).alarmItem(cardItem).build();
        String cardItemString = ProjectMenuDao.JSON_MAPPER.writeValueAsString(cardAlarm);
        System.out.println(cardItemString);
        ProjectAlarmExt alarm2 = ProjectMenuDao.JSON_MAPPER.readValue(cardItemString, ProjectAlarmExt.class);

        Assert.assertEquals(projectId, alarm2.getProjectId());
        Assert.assertEquals("卡片结束提前预警", alarm2.getAlarmItem().getName());
        Assert.assertEquals("endTime", alarm.getAlarmItem().getDateFieldKey());

        NoticeUserConfig noticeUserConfig2 = alarm.getAlarmItem().getWarningUsers().get(0);
        Assert.assertTrue(noticeUserConfig instanceof NoticeFieldUsersConfig);
        NoticeFieldUsersConfig fieldUserConfig2 = (NoticeFieldUsersConfig) noticeUserConfig2;
        Assert.assertTrue(CollectionUtils.isEqualCollection(fieldUserConfig2.getUserFields(), projectFieldUser.getUserFields()));

        NoticeUserConfig noticeUserConfig3 = alarm.getAlarmItem().getWarningUsers().get(1);
        Assert.assertTrue(noticeUserConfig3 instanceof NoticeSpecUsersConfig);
        NoticeSpecUsersConfig specUserConfig = (NoticeSpecUsersConfig) noticeUserConfig3;
        Assert.assertTrue(CollectionUtils.isEqualCollection(specUserConfig.getUsers(), Arrays.asList("ycf", "zhangSan")));
    }
}