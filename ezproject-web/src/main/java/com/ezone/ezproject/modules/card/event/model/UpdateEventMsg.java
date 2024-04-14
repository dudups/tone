package com.ezone.ezproject.modules.card.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateEventMsg implements EventMsg {
    @Singular
    private List<FieldMsg> fieldMsgs;
    @Singular
    private List<FieldDetailMsg> fieldDetailMsgs;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FieldMsg {
        private String fieldKey;
        private String fieldMsg;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class FieldDetailMsg extends FieldMsg {
        private String fromMsg;
        private String toMsg;
    }
}
