package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

/**
 * 值的合法性校验；分两大类：1. 关系型引用字段，校验所引用实体是否合法；2. 一般字段只校验option相关的枚举值，值的数据类型校验可以靠底层es，但列表还是得靠业务验证；
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class FieldValueCheckHelper {
    private Long projectId;
    private Function<Long, Plan> findPlanById;
    private Function<Long, Card> findCardById;
    private Function<Long, StoryMapNode> findStoryMapNodeById;

    public void check(CardField field, Object value) throws CodedException {
        switch (field.getKey()) {
            case CardField.REPO:
            case CardField.TYPE:
            case CardField.STATUS:
                break;
            case CardField.TITLE:
                if (StringUtils.isEmpty(FieldUtil.toString(value))) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "标题不能为空!");
                }
                if (value.toString().length() > 200) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("标题过长:[%s]", field.getName()));
                }
                break;
            case CardField.PLAN_ID:
                PlanIdFieldValueChecker.builder().projectId(projectId).findPlanById(findPlanById).build().check(value);
                break;
            case CardField.PARENT_ID:
                ParentIdFieldValueChecker.builder().projectId(projectId).findCardById(findCardById).build().check(value);
                break;
            case CardField.STORY_MAP_NODE_ID:
                StoryMapNodeIdFieldValueChecker.builder().projectId(projectId).findStoryMapNodeById(findStoryMapNodeById).build().check(value);
                break;
            default:
                switch (field.getType()) {
                    case SELECT:
                    case RADIO:
                        if (!FieldUtil.checkInOptions(value, field.getOptions())) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段值非法:[%s]", field.getName()));
                        }
                        break;
                    case CHECK_BOX:
                        if (!FieldUtil.checkAllInOptions(value, field.getOptions())) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("字段值非法:[%s]", field.getName()));
                        }
                        break;
                    case LINE:
                        if (value != null && value.toString().length() > 200) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("长度过长:[%s]", field.getName()));
                        }
                        break;
                    case LINES:
                        if (value != null && value.toString().length() > 20000) {
                            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("长度过长:[%s]", field.getName()));
                        }
                        break;
                }
                if (value != null) {
                    FieldUtil.parse(field.getValueType(), value);
                }
        }
    }
}
