package com.ezone.ezproject.modules.card.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardEvent {
    private Long id;
    private Long cardId;
    private Date date;
    private Date nextDate;
    private String user;
    private EventType eventType;
    private EventMsg eventMsg;
    private Map<String, Object> cardDetail;

    public static final String CARD_DETAIL = "cardDetail";
    public static final String CARD_ID = "cardId";
    public static final String DATE = "date";
    public static final String NEXT_DATE = "nextDate";
    public static final String EVENT_TYPE = "eventType";

    public static String cardProp(String prop) {
        return StringUtils.joinWith(".", CARD_DETAIL, prop);
    }

    public static BoolQueryBuilder timeQueryBuilder(Date timestamp) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(timestamp));
        bool.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(timestamp))
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                .minimumShouldMatch(1));
        return bool;
    }

    public static BoolQueryBuilder timeQueryBuilder(Date start, Date end) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        bool.filter(QueryBuilders.rangeQuery(CardEvent.DATE).lte(end));
        bool.filter(QueryBuilders.boolQuery()
                .should(QueryBuilders.rangeQuery(CardEvent.NEXT_DATE).gte(start))
                .should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(CardEvent.NEXT_DATE)))
                .minimumShouldMatch(1)
        );
        return bool;
    }
}
