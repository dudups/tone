package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ParentIdFieldValueChecker implements IFieldValueChecker {
    private Long projectId;
    private Function<Long, Card> findCardById;

    @Override
    public void check(Object parentId) throws CodedException {
        Card card = findCardById.apply(NumberUtils.createLong(String.valueOf(parentId)));
        if (null != card && !card.getProjectId().equals(projectId)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Invalid parent!");
        }
    }
}
