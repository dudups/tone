package com.ezone.ezproject.modules.card.bean.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NotEq implements Query {
    @Size(min = 1)
    private String field;

    private String value;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        return QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(fieldConverter.apply(field), value));
    }

    @Override
    public List<String> fields() {
        List<String> fields = new ArrayList<>();
        fields.add(field);
        return fields;
    }
}
