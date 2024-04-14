package com.ezone.ezproject.modules.alarm.service;

import com.ezone.ezproject.dal.entity.CardAlarmNoticePlan;
import com.ezone.ezproject.dal.entity.CardAlarmNoticePlanExample;
import com.ezone.ezproject.dal.mapper.ExtCardAlarmNoticePlanMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Transactional
@Service
@Slf4j
public class CardAlarmNoticePlanService {
    private ExtCardAlarmNoticePlanMapper noticePlanMapper;


    public void save(List<CardAlarmNoticePlan> cardAlarmNoticePlans) {
        if (CollectionUtils.isEmpty(cardAlarmNoticePlans)) {
            return;
        }
        noticePlanMapper.batchInsert(cardAlarmNoticePlans);
    }

    public List<CardAlarmNoticePlan> searchBySendTime(Long projectId, int minute) {
        CardAlarmNoticePlanExample example = new CardAlarmNoticePlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andTimestampMinuteLessThanOrEqualTo(minute).andSendFlagEqualTo(0);
        return noticePlanMapper.selectByExample(example);
    }

    public void cleanNoticePlansBeforeDate(@NotNull Date time) {
        int timestampMinute = (int) (time.getTime() / 1000 / 60);
        CardAlarmNoticePlanExample example = new CardAlarmNoticePlanExample();
        example.createCriteria().andTimestampMinuteLessThan(timestampMinute);
        noticePlanMapper.deleteByExample(example);
    }

    public void deleteByAlarmId(Long alarmId) {
        CardAlarmNoticePlanExample example = new CardAlarmNoticePlanExample();
        example.createCriteria().andAlarmIdEqualTo(alarmId);
        noticePlanMapper.deleteByExample(example);
    }

    public void deleteByCardIds(List<Long> cardIds) {
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        CardAlarmNoticePlanExample example = new CardAlarmNoticePlanExample();
        example.createCriteria().andCardIdIn(cardIds);
        noticePlanMapper.deleteByExample(example);
    }

    public void deleteFlag(CardAlarmNoticePlan cardAlarmNoticePlan) {
        cardAlarmNoticePlan.setSendFlag(1);
        noticePlanMapper.updateByPrimaryKey(cardAlarmNoticePlan);
    }
}
