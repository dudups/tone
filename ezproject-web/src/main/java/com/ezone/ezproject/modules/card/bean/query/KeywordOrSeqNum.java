package com.ezone.ezproject.modules.card.bean.query;

import com.ezone.ezproject.es.entity.CardField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class KeywordOrSeqNum implements Query {
    @NotNull
    @Size(min = 1)
    private String values;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        if (StringUtils.isEmpty(values)) {
            return null;
        }
        QueryBuilder keyword = Keyword.builder().values(values).build().queryBuilder(fieldConverter);
        BoolQueryBuilder should  = QueryBuilders.boolQuery().minimumShouldMatch(1);
        String[] array = values.split("[\\s\\p{Zs}]+");
        if (array.length == 1 && NumberUtils.isDigits(array[0])) {
            String value = array[0];
            should.should(QueryBuilders.termQuery(CardField.SEQ_NUM, NumberUtils.toLong(value)));
            should.should(keyword);
        } else {
            return keyword;
        }
        return should;
    }
}
