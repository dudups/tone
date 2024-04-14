package com.ezone.ezproject.modules.company.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.entity.CompanyProjectSchema;
import com.ezone.ezproject.es.entity.ProjectField;
import com.ezone.ezproject.modules.project.util.ProjectFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CompanyProjectSchemaHelper {
    private static final Pattern CUSTOM_FIELD_KEY_PATTERN = Pattern.compile("^custom_(?<num>[\\d]+)_[\\w]+$");

    public static final int MAX_CUSTOM_FIELDS = 50;

    public CompanyProjectSchema generateCustomFieldKey(CompanyProjectSchema schema) throws CodedException {
        if (null == schema) {
            return null;
        }

        List<ProjectField> fields = schema.getFields();
        if (CollectionUtils.isEmpty(fields)) {
            return schema;
        }
        int[] maxCustomFieldNum = new int[] { 0 };
        List<ProjectField> newFields = new ArrayList<>();
        fields.stream()
                .forEach(field -> {
                    String key = field.getKey();
                    if (StringUtils.isEmpty(key)) {
                        newFields.add(field);
                    } else {
                        Matcher matcher = CUSTOM_FIELD_KEY_PATTERN.matcher(field.getKey());
                        if (matcher.find()) {
                            maxCustomFieldNum[0] = Math.max(maxCustomFieldNum[0], NumberUtils.toInt(matcher.group("num")));
                        } else {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("非法的自定义字段key:[] !", field.getKey()));
                        }
                    }
                });
        newFields.forEach(field -> {
            maxCustomFieldNum[0]++;
            if (maxCustomFieldNum[0] > MAX_CUSTOM_FIELDS) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "自定义状态个数过多!");
            }
            field.setKey(String.format("custom_%s_%s", maxCustomFieldNum[0], field.getType().getDefaultValueType().getEsDataType()));
            field.setValueType(field.getType().getDefaultValueType());
        });

        return schema;
    }

    /**
     * 根据卡片schema定义
     * 1. 移除projectExtends的非法字段及其值；否则写入es，最大问题是可能占用将来扩展字段，其取值和schema定义可能冲突；
     */
    public Map<String, Object> preProcessProjectExtendProps(CompanyProjectSchema schema, Map<String, Object> projectExtends) {
        if(MapUtils.isEmpty(projectExtends)){
            return Collections.EMPTY_MAP;
        }
        // remove invalid props
        Map<String, ProjectField> fields = schema.getFields().stream()
                .filter(f -> f.isEnable()).collect(Collectors
                        .toMap(ProjectField::getKey, Function.identity()));
        Iterator<Map.Entry<String, Object>> it = projectExtends.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            ProjectField field = fields.get(key);
            if (field == null) {
                it.remove();
            } else {
                if (ProjectFieldUtil.isEmptyValue(value) && field.isRequired()) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("必填项:[%s]", field.getName()));
                }
                switch (field.getType()) {
                    case SELECT:
                    case RADIO:
                        if (!ProjectFieldUtil.checkInOptions(value, field.getOptions())) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段值非法:[%s]", field.getName()));
                        }
                        break;
                    case CHECK_BOX:
                        if (!ProjectFieldUtil.checkAllInOptions(value, field.getOptions())) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段值非法:[%s]", field.getName()));
                        }
                        break;
                    case LINE:
                        if (value != null && value.toString().length() > 32) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("长度过长:[%s]", field.getName()));
                        }
                        break;
                    case LINES:
                        if (value != null && value.toString().length() > 10000) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("长度过长:[%s]", field.getName()));
                        }
                        break;
                }
            }
        }
        return projectExtends;
    }

}
