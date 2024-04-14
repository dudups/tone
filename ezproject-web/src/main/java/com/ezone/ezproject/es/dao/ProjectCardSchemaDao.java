package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.action.get.MultiGetResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectCardSchemaDao extends AbstractEsDocDao<Long, ProjectCardSchema> {
    @Override
    protected String index() {
        return EsIndexUtil.indexForProjectCardSchema();
    }

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectSchemaService.getProjectCardSchema"},
            key = "#id"
    )
    @Override
    public void saveOrUpdate(Long id, ProjectCardSchema schema) throws IOException {
        setDocSourceYaml(id, schema.yaml());
    }

    public Map<Long, ProjectCardSchema> find(List<Long> ids, String... fields) throws IOException {
        MultiGetResponse response = getDocSources(index(), ids, fields);
        return Arrays.stream(response.getResponses())
                .filter(r -> r.getResponse().isExists())
                .collect(Collectors.toMap(
                        r -> NumberUtils.createLong(r.getId()),
                        r -> deserializer().apply(r.getResponse().getSourceAsString())));
    }

    /**
     * 添加项目设置中的卡片类型
     * @param id 企业项目设置模板ID
     * @param typeTemplateKey 需要拷贝的源卡片类型的key
     * @param name 新卡片类型的名称
     * @param description 新卡片类型的描述
     * @throws IOException
     */
    public void addCardType(Long id,String typeTemplateKey, String name, String description) throws IOException {
        ProjectCardSchema projectCardSchema = find(id);
        List<CardType> types = projectCardSchema.getTypes();

        CardType.CardTypeBuilder addBuilder =  CardType.builder();
        Optional<CardType> cpTargetCardType = types.stream()
                .filter(cardType -> cardType.getKey().equalsIgnoreCase(typeTemplateKey))
                .findFirst();
        cpTargetCardType.ifPresent(cardType -> {
            addBuilder.key(cardType.getKey())
                    .name(name)
                    .source(Source.CUSTOM)
                    .description(description)
                    .enable(false)
                    .fields(cardType.getFields())
                    .statuses(cardType.getStatuses())
                    .autoStatusFlows(cardType.getAutoStatusFlows());
        });
        types.add(addBuilder.build());
        saveOrUpdate(id, projectCardSchema);
    }


    @Override
    protected @NotNull Function<String, ProjectCardSchema> deserializer() {
        return s -> {
            try {
                return ProjectCardSchema.from(s);
            } catch (JsonProcessingException e) {
                log.error("Deserialize ProjectCardSchema json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
