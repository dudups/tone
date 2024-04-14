package com.ezone.ezproject.modules.chart.config;

import com.ezone.ezproject.modules.card.event.model.CardEvent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public class CardEventsTimeSpans {
    private List<CardEvent> events = ListUtils.EMPTY_LIST;

    private int currentIndex = 0;
    private CardEvent currentEvent = null;
    private CardEvent nextEvent = null;

    /**
     * @param events 时间升序
     */
    public CardEventsTimeSpans(List<CardEvent> events) {
        this.events = events;
        if (CollectionUtils.isNotEmpty(events)) {
            this.currentEvent = events.get(0);
            if (events.size() >= 2) {
                this.nextEvent = events.get(1);
            }
        }
    }

    public CardEvent get(Date timestamp) {
        if (this.currentEvent == null || timestamp.before(this.currentEvent.getDate())) {
            return null;
        }
        if (this.nextEvent != null && this.nextEvent.getDate().before(timestamp)) {
            relocate(timestamp);
        }
        if (this.currentEvent.getNextDate() != null && this.currentEvent.getNextDate().before(timestamp)) {
            return null;
        }
        return this.currentEvent;
    }

    private void relocate(Date timestamp) {
        for (; this.currentIndex < events.size() - 1; ++this.currentIndex) {
            this.currentEvent = this.nextEvent;
            this.nextEvent = events.get(this.currentIndex + 1);
            if (this.nextEvent.getDate().after(timestamp)) {
                return;
            }
        }
        this.currentEvent = this.nextEvent;
        this.nextEvent = null;
    }

    public void reset() {
        this.currentIndex=0;
        if (CollectionUtils.isNotEmpty(events)) {
            this.currentEvent = events.get(0);
            if (events.size() >= 2) {
                this.nextEvent = events.get(1);
            }
        }
    }

    @Nullable
    public CardEvent getFirstEvent() {
        if(events!=null && !events.isEmpty()){
            return events.get(0);
        }else{
            return null;
        }
    }
}
