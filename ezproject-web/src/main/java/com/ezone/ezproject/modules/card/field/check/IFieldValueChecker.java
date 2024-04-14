package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;

public interface IFieldValueChecker {
    void check(Object value) throws CodedException;

    IFieldValueChecker DEFAULT = new IFieldValueChecker() {
        @Override
        public void check(Object value) throws CodedException { }
    };
}
