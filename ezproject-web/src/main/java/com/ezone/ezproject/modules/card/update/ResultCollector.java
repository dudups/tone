package com.ezone.ezproject.modules.card.update;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
public class ResultCollector {
    private int successCount = 0;
    private Map<Long, String> failures = new HashMap<>();

    public void addSuccess() {
        this.successCount++;
    }

    public void addFailure(Long id, String error) {
        this.failures.put(id, error);
    }
}
