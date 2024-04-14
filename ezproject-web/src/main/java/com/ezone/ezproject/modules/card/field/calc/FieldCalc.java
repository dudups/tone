package com.ezone.ezproject.modules.card.field.calc;

import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.common.OperationContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.SetUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldCalc {
    @NotEmpty
    private String field;
    @NotEmpty
    private Set<String> upFields = new HashSet<>();
    @NotNull
    private Calc calc;

    public static final List<FieldCalc> graph = Arrays.asList(
            FieldCalc.builder()
                    .field(CardField.PROJECT_IS_ACTIVE)
                    .upFields(SetUtils.hashSet(CardField.PROJECT_ID))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        Project project = refLoader.project(FieldUtil.getProjectId(cardDetail));
                        boolean fromValue = FieldUtil.getProjectIsActive(cardDetail);
                        boolean toValue = project == null || project.getIsActive() == Boolean.TRUE;
                        cardDetail.put(CardField.PROJECT_IS_ACTIVE, toValue);
                        return fromValue != toValue;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.PLAN_IS_ACTIVE)
                    .upFields(SetUtils.hashSet(CardField.PLAN_ID))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        Plan plan = refLoader.plan(FieldUtil.getPlanId(cardDetail));
                        boolean fromValue = FieldUtil.getPlanIsActive(cardDetail);
                        boolean toValue = plan == null || plan.getIsActive() == Boolean.TRUE;
                        cardDetail.put(CardField.PLAN_IS_ACTIVE, toValue);
                        return fromValue != toValue;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.FIRST_PLAN_ID)
                    .upFields(SetUtils.hashSet(CardField.PLAN_ID))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        Long fromValue = FieldUtil.getFirstPlanId(cardDetail);
                        if (fromValue != null && fromValue > 0) {
                            return false;
                        }
                        Long toValue = FieldUtil.getPlanId(cardDetail);
                        if (toValue == null || toValue <= 0) {
                            return false;
                        }
                        cardDetail.put(CardField.FIRST_PLAN_ID, toValue);
                        return true;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.CALC_IS_END)
                    .upFields(SetUtils.hashSet(CardField.TYPE, CardField.STATUS))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        boolean fromValue = FieldUtil.getCalcIsEnd(cardDetail);
                        boolean toValue = schema.isEndStatus(cardDetail);
                        cardDetail.put(CardField.CALC_IS_END, toValue);
                        return fromValue != toValue;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.LAST_END_TIME)
                    .upFields(SetUtils.hashSet(CardField.CALC_IS_END))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        if (FieldUtil.getCalcIsEnd(cardDetail)) {
                            cardDetail.put(CardField.LAST_END_TIME, context.getCurrentTimeMillis());
                            return true;
                        }
                        return false;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.LAST_END_DELAY)
                    .upFields(SetUtils.hashSet(CardField.CALC_IS_END, CardField.LAST_END_TIME, CardField.END_DATE))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        Long endDate = FieldUtil.getEndDate(cardDetail);
                        Long lastEndTime = FieldUtil.getLastEndTime(cardDetail);
                        boolean lastEndDelay = endDate != null && endDate > 0 && lastEndTime != null && endDate < lastEndTime;
                        if (FieldUtil.getLastEndDelay(cardDetail) != lastEndDelay) {
                            cardDetail.put(CardField.LAST_END_DELAY, lastEndDelay);
                            return true;
                        }
                        return false;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.INNER_TYPE)
                    .upFields(SetUtils.hashSet(CardField.TYPE))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        String type = FieldUtil.getType(cardDetail);
                        CardType cardType = schema.findCardType(type);
                        String fromValue = FieldUtil.getInnerType(cardDetail);
                        String toValue = cardType.getInnerType().name();
                        if (toValue.equals(fromValue)) {
                            return true;
                        }
                        cardDetail.put(CardField.INNER_TYPE, toValue);
                        return true;
                    })
                    .build(),
            FieldCalc.builder()
                    .field(CardField.LAST_STATUS_MODIFY_TIME)
                    .upFields(SetUtils.hashSet(CardField.STATUS))
                    .calc((cardDetail, context, schema, refLoader) -> {
                        cardDetail.put(CardField.LAST_STATUS_MODIFY_TIME, context.getCurrentTimeMillis());
                        return true;
                    })
                    .build()
    );

    interface Calc {
        /**
         * 1. for create or update
         */
        boolean calc(Map<String, Object> cardDetail, OperationContext context, ProjectCardSchema schema, FieldRefValueLoader refLoader);
    }

}
