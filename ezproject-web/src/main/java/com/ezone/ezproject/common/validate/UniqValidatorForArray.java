package com.ezone.ezproject.common.validate;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.springframework.http.HttpStatus;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Slf4j
@NoArgsConstructor
public class UniqValidatorForArray implements ConstraintValidator<Uniq, Object[]> {
    private String field;
    private boolean ignoreEmpty;

    @Override
    public void initialize(Uniq parameters) {
        this.field = parameters.field();
        this.ignoreEmpty = parameters.ignoreEmpty();
    }

    @Override
    public boolean isValid(Object[] array, ConstraintValidatorContext constraintValidatorContext) {
        if (array == null || array.length <= 1) {
            return true;
        } else {
            Function getter = StringUtils.isEmpty(field) ? Function.identity() : this::readFieldValue;
            Set values = new HashSet();
            int size = 0;
            for (Object obj : array) {
                Object value = getter.apply(obj);
                if (value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
                    continue;
                }
                size++;
                values.add(value);
            }
            return size == values.size();
        }
    }

    private Object readFieldValue(Object o) {
        try {
            return FieldUtils.readDeclaredField(o, field, true);
        } catch (IllegalAccessException e) {
            log.error("Read field value Exception!", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
