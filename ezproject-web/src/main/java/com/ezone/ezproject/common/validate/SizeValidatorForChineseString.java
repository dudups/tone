package com.ezone.ezproject.common.validate;

import lombok.NoArgsConstructor;
import org.hibernate.validator.internal.util.logging.Log;
import org.hibernate.validator.internal.util.logging.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.invoke.MethodHandles;

@NoArgsConstructor
public class SizeValidatorForChineseString implements ConstraintValidator<ChineseStringSize, CharSequence> {
    private static final Log LOG = LoggerFactory.make(MethodHandles.lookup());
    private int min;
    private int max;

    @Override
    public void initialize(ChineseStringSize parameters) {
        this.min = parameters.min();
        this.max = parameters.max();
        this.validateParameters();
    }

    @Override
    public boolean isValid(CharSequence charSequence, ConstraintValidatorContext constraintValidatorContext) {
        if (charSequence == null) {
            return true;
        } else {
            int length = length(charSequence);
            return length >= this.min && length <= this.max;
        }
    }

    private int length(CharSequence charSequence) {
        // 和前端保持一致，\u0391-\uFFE5区间的中文长度算2
        return charSequence.chars().map(c -> c > '\u0391' && c < '\uFFE5' ? 2 : 1).sum();
    }

    private void validateParameters() {
        if (this.min < 0) {
            throw LOG.getMinCannotBeNegativeException();
        } else if (this.max < 0) {
            throw LOG.getMaxCannotBeNegativeException();
        } else if (this.max < this.min) {
            throw LOG.getLengthCannotBeNegativeException();
        }
    }
}
