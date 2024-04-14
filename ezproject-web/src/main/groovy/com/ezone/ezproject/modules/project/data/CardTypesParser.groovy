package com.ezone.ezproject.modules.project.data

import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.filter.ParsedFilter
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality

import java.util.stream.Collectors

class CardTypesParser {
    List<String> parse(Aggregations aggs) {
        def cardTypes = aggs['cardTypes'] as ParsedStringTerms
        if (cardTypes.buckets) {
            return cardTypes.buckets.stream().map({it.key}).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }
}
