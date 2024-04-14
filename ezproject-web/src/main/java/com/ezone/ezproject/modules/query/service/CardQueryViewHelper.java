package com.ezone.ezproject.modules.query.service;

import com.ezone.ezproject.dal.entity.CardQueryView;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Component
public class CardQueryViewHelper {
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Getter(lazy = true)
    private final byte[] initCardQueryViewTemplate = initCardQueryViewTemplate();

    private byte[] initCardQueryViewTemplate() {
        try {
            return IOUtils.toByteArray(
                    CardQueryView.class.getResource("/init-card-view-template.yaml"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public SearchEsRequest initCardQueryView(Query... queries) throws IOException {
        SearchEsRequest view = YAML_MAPPER.readValue(
                getInitCardQueryViewTemplate(),
                SearchEsRequest.class
        );
        if (queries == null || queries.length == 0) {
            return view;
        }
        List<Query> allQueries = new ArrayList<>(view.getQueries());
        for (Query query : queries) {
            allQueries.add(query);
        }
        view.setQueries(allQueries);
        return view;
    }
}
