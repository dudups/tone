package com.ezone.ezproject.modules.card.bean.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Contains implements Query {
    @Size(min = 1)
    private String field;

    private String values;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        if (StringUtils.isEmpty(values)) {
            return null;
        }
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        Arrays.stream(values.split("[\\s\\p{Zs}]+")).filter(s -> s != null).forEach(s -> {
            bool.filter(QueryBuilders.wildcardQuery(fieldConverter.apply(field), String.format("*%s*", s)));
        });
        return bool;
    }

    @Override
    public List<String> fields() {
        List<String> fields = new ArrayList<>();
        fields.add(field);
        return fields;
    }
}
