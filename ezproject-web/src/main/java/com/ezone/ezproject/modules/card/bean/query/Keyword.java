package com.ezone.ezproject.modules.card.bean.query;

import com.ezone.ezproject.es.entity.CardField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Keyword implements Query {
    @NotNull
    @Size(min = 1)
    private String values;

    @Builder.Default
    private List<String> fields = FIELDS;

    private static final List<String> FIELDS = Arrays.asList(CardField.TITLE, CardField.CONTENT);

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        if (StringUtils.isEmpty(values)) {
            return null;
        }
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        Arrays.stream(values.split("[\\s\\p{Zs}]+")).filter(s -> s != null).forEach(s -> {
            BoolQueryBuilder should = QueryBuilders.boolQuery().minimumShouldMatch(1);
            fields.forEach(field -> should.should(QueryBuilders.wildcardQuery(fieldConverter.apply(field), String.format("*%s*", s))));
            bool.filter(should);
        });
        return bool;
    }
}
