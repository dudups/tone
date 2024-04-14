package com.ezone.ezproject.modules.card.event.service;

import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.event.AbstractEventListener;
import com.ezone.ezproject.modules.event.events.AttachmentCreateEvent;
import com.ezone.ezproject.modules.event.events.AttachmentDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardCreateEvent;
import com.ezone.ezproject.modules.event.events.CardDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardUpdateEvent;
import com.ezone.ezproject.modules.event.events.CardsCloseStatusEvent;
import com.ezone.ezproject.modules.event.events.CardsCreateEvent;
import com.ezone.ezproject.modules.event.events.CardsDeleteEvent;
import com.ezone.ezproject.modules.event.events.CardsRecoveryEvent;
import com.ezone.ezproject.modules.event.events.CardsUpdateEvent;
import com.ezone.ezproject.modules.event.events.RelateCardAddEvent;
import com.ezone.ezproject.modules.event.events.RelateCardRmEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@AllArgsConstructor
@Slf4j
@Service
public class CardEventListener extends AbstractEventListener {
    private CardEventCmdService cardEventCmdService;

    @PostConstruct
    private void init() {
        // create
        registerEventConsumer(
                CardCreateEvent.class,
                e -> cardEventCmdService.asyncSaveForCreate(e.getCard().getId(), e.getUser(), e.getCardDetail())
        );
        registerEventConsumer(
                CardsCreateEvent.class,
                e -> cardEventCmdService.asyncSaveForCreate(e.getUser(), e.getCardDetails())
        );
        // update
        registerEventConsumer(
                CardUpdateEvent.class,
                e -> cardEventCmdService.asyncSave(e.getCardEvent())
        );
        registerEventConsumer(
                CardsUpdateEvent.class,
                e -> cardEventCmdService.asyncSave(e.getCardEvents())
        );
        registerEventConsumer(
                CardsCloseStatusEvent.class,
                e -> cardEventCmdService.asyncSave(e.getCardEvents())
        );
        // delete
        registerEventConsumer(
                CardDeleteEvent.class,
                e -> cardEventCmdService.asyncSaveForDelete(e.getCard().getId(), e.getUser(), e.getCardDetail())
        );
        registerEventConsumer(
                CardsDeleteEvent.class,
                e -> cardEventCmdService.asyncSaveForDelete(e.getUser(), e.getCardDetails())
        );
        // recovery
        registerEventConsumer(
                CardRecoveryEvent.class,
                e -> cardEventCmdService.asyncSaveForRecovery(e.getCard().getId(), e.getUser(), e.getCardDetail())
        );
        registerEventConsumer(
                CardsRecoveryEvent.class,
                e -> cardEventCmdService.asyncSaveForRecovery(e.getUser(), e.getCardDetails())
        );
        // attachment
        registerEventConsumer(
                AttachmentCreateEvent.class,
                e -> cardEventCmdService.asyncSaveForAddAttachment(e.getCard().getId(), e.getUser(), e.getAttachment().getFileName(), e.getCardDetail())
        );
        registerEventConsumer(
                AttachmentDeleteEvent.class,
                e -> cardEventCmdService.asyncSaveForRmAttachment(e.getCard().getId(), e.getUser(), e.getAttachment().getFileName(), e.getCardDetail())
        );
        // relate card
        registerEventConsumer(
                RelateCardAddEvent.class,
                e -> cardEventCmdService.asyncSaveForAddRelateCard(e.getCard().getId(), e.getUser(), CardHelper.cardKey(e.getRelateCard()), e.getCardDetail())
        );
        registerEventConsumer(
                RelateCardRmEvent.class,
                e -> cardEventCmdService.asyncSaveForRmRelateCard(e.getCard().getId(), e.getUser(), CardHelper.cardKey(e.getRelateCard()), e.getCardDetail())
        );
    }
}
