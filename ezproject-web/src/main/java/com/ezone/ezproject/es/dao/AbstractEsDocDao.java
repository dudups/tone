package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.EsUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.modules.card.bean.query.Query;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.xcontent.XContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class AbstractEsDocDao<K, O> extends AbstractEsBaseDao<K, O> {
    protected abstract String index();

    public abstract void saveOrUpdate(K id, O o) throws IOException;

    public O find(K id) throws IOException {
        return getDocSourceObject(index(), id);
    }

    public O find(K id, String... fields) throws IOException {
        return getDocSourceObject(index(), id, fields);
    }

    public List<O> find(List<K> ids) throws IOException {
        return getDocSourceObject(index(), ids);
    }

    public void delete(K id) throws IOException {
        deleteDoc(index(), id);
    }

    public void delete(List<K> ids) throws IOException {
        deleteDoc(index(), ids);
    }

    protected String getDocSource(K docId) throws IOException {
        return getDocSource(index(), docId);
    }

    protected void setDocSourceYaml(K docId, String yaml) throws CodedException, IOException {
        setDocSource(docId, yaml, XContentType.YAML);
    }

    protected void setDocSourceJson(K docId, String json) throws CodedException, IOException {
        setDocSource(docId, json, XContentType.JSON);
    }

    protected void setDocSource(K docId, String docSource, XContentType docContentType) throws CodedException, IOException {
        setDocSource(index(), docId, docSource, docContentType);
    }

    public static String SCRIPT_SET_FIELD = "ctx._source[params.field]=params.value";

    public static Script setFieldScript(String field, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put("field", field);
        params.put("value", value);
        return new Script(
                ScriptType.INLINE,
                Script.DEFAULT_SCRIPT_LANG,
                SCRIPT_SET_FIELD,
                params);
    }

    protected void updateByQuery(List<Query> queries, Function<String, String> fieldConverter, Script script) throws IOException {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();
        queryBuilders(queries, fieldConverter).forEach(bool::filter);
        UpdateByQueryRequest request = new UpdateByQueryRequest(index());
        request.setQuery(bool);
        request.setScript(script);
        request.setTimeout(EsUtil.TIME_OUT);
        request.setBatchSize(10000);
        es.updateByQuery(request, EsUtil.REQUEST_OPTIONS);
    }

    protected void updateByQuery(List<Query> queries, Script script) throws IOException {
        updateByQuery(queries, Function.identity(), script);
    }
}
