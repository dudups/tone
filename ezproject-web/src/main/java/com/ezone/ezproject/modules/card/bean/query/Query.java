package com.ezone.ezproject.modules.card.bean.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.QueryBuilder;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "and", value = And.class),
        @JsonSubTypes.Type(name = "between", value = Between.class),
        @JsonSubTypes.Type(name = "contains", value = Contains.class),
        @JsonSubTypes.Type(name = "eq", value = Eq.class),
        @JsonSubTypes.Type(name = "exist", value = Exist.class),
        @JsonSubTypes.Type(name = "gt", value = Gt.class),
        @JsonSubTypes.Type(name = "gte", value = Gte.class),
        @JsonSubTypes.Type(name = "ids", value = Ids.class),
        @JsonSubTypes.Type(name = "in", value = In.class),
        @JsonSubTypes.Type(name = "keyword", value = Keyword.class),
        @JsonSubTypes.Type(name = "keywordOrSeqNum", value = KeywordOrSeqNum.class),
        @JsonSubTypes.Type(name = "lt", value = Lt.class),
        @JsonSubTypes.Type(name = "lte", value = Lte.class),
        @JsonSubTypes.Type(name = "notContains", value = NotContains.class),
        @JsonSubTypes.Type(name = "notEq", value = NotEq.class),
        @JsonSubTypes.Type(name = "notExist", value = NotExist.class),
        @JsonSubTypes.Type(name = "notIn", value = NotIn.class),
        @JsonSubTypes.Type(name = "or", value = Or.class),
        @JsonSubTypes.Type(name = "seqNumOrTitle", value = SeqNumOrTitle.class),
        @JsonSubTypes.Type(name = "stakeholder", value = Stakeholder.class),
})
public interface Query {

    default QueryBuilder queryBuilder() {
        return queryBuilder(Function.identity());
    }

    default QueryBuilder queryBuilder(String fieldContextPath) {
        return queryBuilder(field -> StringUtils.joinWith(".", fieldContextPath, field));
    }

    QueryBuilder queryBuilder(Function<String, String> fieldConverter);

    @JsonIgnore
    default List<String> fields() {
        return Collections.emptyList();
    }


    // 注意避开静态初始化死锁，这种情况jstack只能看到线程信息行xxx in Object.wait()，以及状态是runnable
//    List<Query> EXAMPLES = Arrays.asList(
//            Between.builder().field("f1").start("1").end("2").build(),
//            Contains.builder().field("f1").values("v1 v2").build(),
//            Eq.builder().field("f1").value("abc").build(),
//            Exist.builder().field("f1").build(),
//            Gt.builder().field("f1").value("1").build(),
//            Gte.builder().field("f1").value("1").build(),
//            In.builder().field("f1").values(Arrays.asList("a", "b")).build(),
//            Keyword.builder().values("a b").build(),
//            KeywordOrSeqNum.builder().values("a b").build(),
//            Lt.builder().field("f1").value("1").build(),
//            Lte.builder().field("f1").value("1").build(),
//            NotContains.builder().field("f1").values("v1 v2").build(),
//            NotEq.builder().field("f1").value("abc").build(),
//            NotExist.builder().field("f1").build(),
//            NotIn.builder().field("f1").values(Arrays.asList("a", "b")).build()
//    );
}
