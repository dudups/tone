package com.ezone.ezproject.modules.card.event.service;

import com.ezone.ezproject.modules.alarm.service.AlarmNoticeService;
import com.ezone.ezproject.modules.card.service.CardCreateUserNoticeService;
import com.ezone.ezproject.modules.card.service.CardMemberChangedNoticeService;
import com.ezone.ezproject.modules.event.AbstractEventListener;
import com.ezone.ezproject.modules.event.events.CardCreateEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsCreateEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@AllArgsConstructor
@Slf4j
@Service
public class CardEventUserNoticeListener extends AbstractEventListener {
    private CardMemberChangedNoticeService cardMemberChangedNoticeService;
    private CardCreateUserNoticeService cardCreateUserNoticeService;

    @PostConstruct
    private void init() {
        // update
        registerEventConsumer(
                CardUpdateEvent.class,
                cardMemberChangedNoticeService::noticeCardUpdate
        );
        registerEventConsumer(
                CardsUpdateEvent.class,
                cardMemberChangedNoticeService::noticeCardsUpdate
        );

        registerEventConsumer(
                CardsCreateEvent.class,
                cardCreateUserNoticeService::noticeCardsCreate
        );
        registerEventConsumer(
                CardCreateEvent.class,
                cardCreateUserNoticeService::noticeCardCreate
        );
    }
}
