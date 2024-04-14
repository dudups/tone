package com.ezone.ezproject.es.dao;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectTemplateDetail;
import com.ezone.ezproject.es.entity.enums.Source;
import com.ezone.ezproject.es.util.EsIndexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectTemplateDetailDao extends AbstractEsDocDao<Long, ProjectTemplateDetail> {
    @Override
    protected String index() {
        return EsIndexUtil.indexForProjectTemplateDetail();
    }

    @Override
    public void saveOrUpdate(Long id, ProjectTemplateDetail detail) throws IOException {
        setDocSourceYaml(id, detail.yaml());
    }

    /**
     * 添加企业项目设置中的卡片类型
     * @param id 企业项目设置模板ID
     * @param typeTemplateKey 需要拷贝的源卡片类型的key
     * @param name 新卡片类型的名称
     * @param description 新卡片类型的描述
     * @throws IOException
     */
    public void addCardType(Long id,String typeTemplateKey, String name, String description) throws IOException {
        ProjectTemplateDetail projectTemplateDetail = find(id);
        List<CardType> types = projectTemplateDetail.getProjectCardSchema().getTypes();

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
        setDocSourceYaml(id, projectTemplateDetail.yaml());
    }

    @Override
    protected @NotNull Function<String, ProjectTemplateDetail> deserializer() {
        return s -> {
            try {
                return ProjectTemplateDetail.from(s);
            } catch (JsonProcessingException e) {
                log.error("Deserialize ProjectTemplateDetail json exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        };
    }
}
