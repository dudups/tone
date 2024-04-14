package com.ezone.ezproject.modules.card.update;

import com.ezone.ezproject.modules.card.field.bean.FieldChange;

import java.util.Map;

public interface Checker {
    boolean checkUpdate(final Map<String, Object> cardDetails, final FieldChange fieldChanges);
}
