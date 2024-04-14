package com.ezone.ezproject.modules.card.event.controller;

import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.external.ci.bean.AutoStatusFlowEventType;
import com.ezone.ezproject.modules.card.event.model.AddAttachmentEventMsg;
import com.ezone.ezproject.modules.card.event.model.AddRelateCardEventMsg;
import com.ezone.ezproject.modules.card.event.model.AutoStatusFlowEventMsg;
import com.ezone.ezproject.modules.card.event.model.CardEvent;
import com.ezone.ezproject.modules.card.event.model.CreateEventMsg;
import com.ezone.ezproject.modules.card.event.model.DelAttachmentEventMsg;
import com.ezone.ezproject.modules.card.event.model.DelRelateCardEventMsg;
import com.ezone.ezproject.modules.card.event.model.EventType;
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg;
import com.ezone.ezproject.modules.card.event.service.CardEventQueryService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiOperation("卡片卡片模版")
@RestController
@RequestMapping("/project/card/{cardId:[0-9]+}")
@Slf4j
@AllArgsConstructor
public class CardEventController extends AbstractController {
    private CardEventQueryService cardEventQueryService;

    private CardQueryService cardQueryService;

    @ApiOperation("查询卡片事件: 返回各种类型事件请参看接口'event/example'")
    @GetMapping("event/{eventId:[0-9]+}")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<CardEvent> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId,
                                                   @ApiParam(value = "事件ID", example = "1") @PathVariable Long eventId)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkHasProjectRead(card.getProjectId());
        return success(cardEventQueryService.select(eventId));
    }

    @ApiOperation("查询卡片事件: 返回各种类型事件请参看接口'event/example'")
    @GetMapping("event")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<CardEvent>> select(@ApiParam(value = "卡片ID", example = "1") @PathVariable Long cardId)
            throws IOException {
        Card card = cardQueryService.select(cardId);
        checkHasProjectRead(card.getProjectId());
        return success(cardEventQueryService.selectByCardId(cardId));
    }

    @ApiOperation("卡片事件Example")
    @GetMapping("event/example")
    public List<CardEvent> example(@ApiParam(example = "1") @PathVariable Long cardId) {
        return CARD_EVENTS_EXAMPLES;
    }

    private static final Map<String, Object> CARD_DETAIL = new HashMap<String, Object>() {{
        put("type", "story");
        put("title", "s1");
    }};

    private static final List<CardEvent> CARD_EVENTS_EXAMPLES = Arrays.asList(
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.CREATE)
                    .eventMsg(CreateEventMsg.builder()
                            .title("card1")
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.UPDATE)
                    .eventMsg(UpdateEventMsg.builder()
                            .fieldMsg(UpdateEventMsg.FieldMsg.builder()
                                    .fieldKey("content")
                                    .fieldMsg("内容")
                                    .build())
                            .fieldDetailMsg(UpdateEventMsg.FieldDetailMsg.builder()
                                    .fieldKey("key")
                                    .fieldMsg("标题")
                                    .fromMsg("card-demo")
                                    .toMsg("card-test")
                                    .build())
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.ADD_ATTACHMENT)
                    .eventMsg(AddAttachmentEventMsg.builder()
                            .fileName("doc.txt")
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.RM_ATTACHMENT)
                    .eventMsg(DelAttachmentEventMsg.builder()
                            .fileName("doc.txt")
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.DELETE)
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.RECOVERY)
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.AUTO_STATUS_FLOW)
                    .eventMsg(AutoStatusFlowEventMsg.builder()
                            .eventType(AutoStatusFlowEventType.CODE_REVIEW_ADD)
                            .fromMsg("新建")
                            .toMsg("进行中")
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.ADD_RELATE_CARD)
                    .eventMsg(AddRelateCardEventMsg.builder()
                            .cardKey("s2")
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build(),
            CardEvent.builder()
                    .id(1L)
                    .cardId(1L)
                    .user("u1")
                    .date(new Date())
                    .eventType(EventType.RM_RELATE_CARD)
                    .eventMsg(DelRelateCardEventMsg.builder()
                            .cardKey("s2")
                            .build())
                    .cardDetail(CARD_DETAIL)
                    .build()
    );

}
