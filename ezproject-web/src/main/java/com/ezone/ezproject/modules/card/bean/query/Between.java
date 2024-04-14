package com.ezone.ezproject.modules.card.bean.query;

import com.ezone.ezproject.modules.chart.config.range.DateRange;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.BooleanUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@ApiModel(parent = Query.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Between implements Query {
    @Size(min = 1)
    private String field;

    private String start;

    private String end;

    private Boolean dynamicDate;

    private DateRange dateRange;

    @Override
    public QueryBuilder queryBuilder(Function<String, String> fieldConverter) {
        if (BooleanUtils.isTrue(dynamicDate)) {
            start = String.valueOf(dateRange.start().getTime());
            end = String.valueOf(dateRange.end().getTime());
        }
        return QueryBuilders.rangeQuery(fieldConverter.apply(field)).from(start).to(end);
    }

    @Override
    public List<String> fields() {
        List<String> fields = new ArrayList<>();
        fields.add(field);
        return fields;
    }
}
