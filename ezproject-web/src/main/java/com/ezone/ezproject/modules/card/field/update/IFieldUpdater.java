package com.ezone.ezproject.modules.card.field.update;

import java.util.Map;

public interface IFieldUpdater {
    void update(String field, Object value, Map<String, Object> json);

    IFieldUpdater DEFAULT = new IFieldUpdater() {
        @Override
        public void update(String field, Object value, Map<String, Object> json) {
            json.put(field, value);
        }
    };
}
