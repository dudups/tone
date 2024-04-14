package com.ezone.ezproject.modules.card.event.service;

import com.ezone.ezproject.modules.card.service.CardNormalNoticeService;
import com.ezone.ezproject.modules.event.AbstractEventListener;
import com.ezone.ezproject.modules.event.events.CardDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardsRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@AllArgsConstructor
@Slf4j
@Service
public class CardNormalNoticeEventListener extends AbstractEventListener {
    private CardNormalNoticeService cardNormalNoticeService;

    @PostConstruct
    private void init() {

        // update
        registerEventConsumer(
                CardUpdateEvent.class,
                cardNormalNoticeService::noticeCardUpdate
        );
        registerEventConsumer(
                CardsUpdateEvent.class,
                cardNormalNoticeService::noticeCardsUpdate
        );
        // delete
        registerEventConsumer(
                CardDeleteEvent.class,
                cardNormalNoticeService::noticeCardDelete
        );
        registerEventConsumer(
                CardsDeleteEvent.class,
                cardNormalNoticeService::noticeCardsDelete
        );
        // recovery
        registerEventConsumer(
                CardRecoveryEvent.class,
                cardNormalNoticeService::noticeCardRecovery
        );
        registerEventConsumer(
                CardsRecoveryEvent.class,
                cardNormalNoticeService::noticeCardsRecovery
        );
    }
}
