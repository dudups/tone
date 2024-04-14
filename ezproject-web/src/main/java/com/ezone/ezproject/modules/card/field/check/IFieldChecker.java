package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;

import java.util.Map;

public interface IFieldChecker {
    void check(Map<String, Object> cardProps) throws CodedException;

    IFieldChecker DEFAULT = new IFieldChecker() {
        @Override
        public void check(Map<String, Object> cardProps) throws CodedException { }
    };
}
