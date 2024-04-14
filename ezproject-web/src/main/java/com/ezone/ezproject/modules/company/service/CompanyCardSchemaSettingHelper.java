package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.project.bean.CardTypeConf;
import com.ezone.ezproject.modules.project.bean.CompanyCardTypeConf;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class CompanyCardSchemaSettingHelper {

    public CompanyCardSchema setTypes(CompanyCardSchema schema, @NotNull List<CompanyCardTypeConf> types) {
        if(CollectionUtils.isEmpty(types)){
            throw CodedException.BAD_REQUEST;
        }
        schema.mergeTypes(types);
        checkTypeNameConflict(schema);
        return schema;
    }

    public void checkTypeNameConflict(CompanyCardSchema schema) {
        List<CompanyCardTypeConf> types = schema.getTypes();
        if (CollectionUtils.isEmpty(types)) {
            return;
        }
        if (types.size() > types.stream().map(f -> f.getName()).distinct().count()) {
            throw new CodedException(HttpStatus.CONFLICT, "类型名冲突！");
        }
    }
}
