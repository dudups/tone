package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class CardFieldStatusesChecker implements IFieldChecker {
    private ProjectCardSchema schema;
    private Map<String, Object> fromCardDetail;
    @Override
    public void check(Map<String, Object> cardDetail) throws CodedException {
        String type = FieldUtil.getType(cardDetail);
        CardType cardType = schema.findCardType(type);
        if (cardType == null) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("卡片类型[%s]不存在！",
                    type));
        }
        List<CardType.FieldConf> fields = cardType.getFields();
        fields.stream()
                .filter(CardType.FieldConf::isEnable)
                .forEach(fieldConf ->
                        FieldStatusLimitHelp.builder()
                                .fieldKey(fieldConf.getKey())
                                .schema(schema)
                                .build()
                                .check(fromCardDetail, cardDetail)
                );
    }


}
