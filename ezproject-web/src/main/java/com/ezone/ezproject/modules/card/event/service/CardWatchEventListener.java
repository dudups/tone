package com.ezone.ezproject.modules.card.event.service;

import com.ezone.ezproject.modules.card.service.CardWatchNoticeService;
import com.ezone.ezproject.modules.event.AbstractEventListener;
import com.ezone.ezproject.modules.event.events.CardDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsCloseStatusEvent;
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
public class CardWatchEventListener extends AbstractEventListener {
    private CardWatchNoticeService cardWatchNoticeService;

    @PostConstruct
    private void init() {
        // update
        registerEventConsumer(
                CardUpdateEvent.class,
                cardWatchNoticeService::noticeCardUpdate
        );
        registerEventConsumer(
                CardsUpdateEvent.class,
                cardWatchNoticeService::noticeCardsUpdate
        );
        registerEventConsumer(
                CardsCloseStatusEvent.class,
                cardWatchNoticeService::noticeCardsUpdate
        );
        // delete
        registerEventConsumer(
                CardDeleteEvent.class,
                cardWatchNoticeService::noticeCardDelete
        );
        registerEventConsumer(
                CardsDeleteEvent.class,
                cardWatchNoticeService::noticeCardsDelete
        );
        // recovery
        registerEventConsumer(
                CardRecoveryEvent.class,
                cardWatchNoticeService::noticeCardRecovery
        );
        registerEventConsumer(
                CardsRecoveryEvent.class,
                cardWatchNoticeService::noticeCardsRecovery
        );
    }
}
