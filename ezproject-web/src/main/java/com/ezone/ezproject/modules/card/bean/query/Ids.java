package com.ezone.ezproject.modules.card.bean.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.Size;
import java.util.Collection;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Ids implements Query {
    @Size(min = 1)
    private Collection<String> ids;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        return QueryBuilders.idsQuery().addIds(ids.stream().toArray(String[]::new));
    }
}
