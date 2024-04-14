package com.ezone.ezproject.modules.card.bean.query;

import com.ezone.ezproject.es.entity.CardField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Stakeholder implements Query {
    @NotNull
    @Size(min = 1)
    private List<String> users;

    private static final List<String> FIELDS = CardField.STAKEHOLDER_FIELD_KEYS;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        if (CollectionUtils.isEmpty(users)) {
            return null;
        }
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        users.stream().filter(user -> user != null).forEach(user -> {
            BoolQueryBuilder should = QueryBuilders.boolQuery().minimumShouldMatch(1);
            FIELDS.forEach(field -> should.should(QueryBuilders.termQuery(fieldConverter.apply(field), user)));
            bool.filter(should);
        });
        return bool;
    }
}
