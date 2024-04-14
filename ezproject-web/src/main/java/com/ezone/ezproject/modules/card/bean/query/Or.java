package com.ezone.ezproject.modules.card.bean.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Or implements Query {
    private List<Query> queries;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        BoolQueryBuilder should = QueryBuilders.boolQuery().minimumShouldMatch(1);
        queries.forEach(query -> should.should(query.queryBuilder(fieldConverter)));
        return should;
    }

    @Override
    public List<String> fields() {
        List<String> fields = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(queries)) {
            for (Query query : queries) {
                fields.addAll(query.fields());
            }
        }
        return fields;
    }
}
