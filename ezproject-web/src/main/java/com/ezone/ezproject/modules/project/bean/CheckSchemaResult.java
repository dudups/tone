package com.ezone.ezproject.modules.project.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CheckSchemaResult {
    @Singular
    private List<String> disabledCardTypes;

    @Singular
    private List<String> incompatibleFields;
    @Singular
    private Map<String, String> compatibleFields;

    @Singular
    private List<String> incompatibleStatuses;
    @Singular
    private Map<String, String> compatibleStatuses;

    @Singular
    private Map<String, CheckCardResult> checkCardResults;

    public void checkCardResult(String cardType, CheckCardResult result) {
        if (null == checkCardResults) {
            checkCardResults = new HashMap<>();
        }
        checkCardResults.put(cardType, result);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CheckCardResult {
        @Singular
        private List<String> incompatibleFields;
        @Singular
        private List<String> disabledFields;

        @Singular
        private List<String> incompatibleStatuses;
        @Singular
        private List<String> disabledStatuses;
        private String defaultStatus;
    }
}
