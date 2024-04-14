package com.ezone.ezproject.modules.project.bean;

import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardStatus;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.chart.config.range.DateRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.ListUtils;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardScatter {
    private DateRange range;
    private List<CardEvent> events = ListUtils.EMPTY_LIST;

    public Data data() {
        Date rangeStartTime = range.start();
        Date startTime = null;
        Date endTime = null;
        Function<CardEvent, Boolean> isEndFunc = isEnd();
        boolean isEnd = false;
        Long seqNum = null;
        Long projectId = null;
        for (CardEvent event : events) {
            if (event.getCardDetail() == null) {
                continue;
            }
            if (seqNum == null) {
                seqNum = FieldUtil.toLong(event.getCardDetail().get(CardField.SEQ_NUM));
            }
            if (projectId == null) {
                projectId = FieldUtil.getProjectId(event.getCardDetail());
            }
            if (startTime == null) {
                if (!isFirstStatus(event)) {
                    startTime = event.getDate();
                }
            }
            if (isEnd) {
                isEnd = isEndFunc.apply(event);
            } else {
                isEnd = isEndFunc.apply(event);
                if (isEnd) {
                    endTime = event.getDate();
                }
            }
        }
        if (endTime == null || endTime.before(rangeStartTime)) {
            return null;
        }
        Long duration = 0L;
        if (startTime != null && endTime != null && startTime.before(endTime)) {
            duration = endTime.getTime() - startTime.getTime();
        }
        return Data.builder()
                .seqNum(seqNum)
                .type(FieldUtil.toString(events.get(events.size() - 1).getCardDetail().get(CardField.TYPE)))
                .timestamp(endTime.getTime())
                .duration(duration)
                .projectId(projectId)
                .build();
    }

    private boolean hasBindPlan(CardEvent event) {
        Long planId = FieldUtil.toLong(event.getCardDetail().get(CardField.PLAN_ID));
        return planId != null && planId > 0;
    }

    private boolean isFirstStatus(CardEvent event) {
        return CardStatus.FIRST.equals(FieldUtil.getStatus(event.getCardDetail()));
    }

    private Function<CardEvent, Boolean> isEnd() {
        return event -> FieldUtil.toBoolean(event.getCardDetail().get(CardField.CALC_IS_END));
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.Data
    @Builder
    public static class Data {
        private Long seqNum;
        private String type;
        private Long timestamp;
        private Long duration;
        private Long projectId;
    }
}
