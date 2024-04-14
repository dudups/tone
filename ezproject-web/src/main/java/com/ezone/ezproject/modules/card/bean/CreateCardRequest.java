package com.ezone.ezproject.modules.card.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateCardRequest {
    @Min(1)
    private Long projectId;
    private Long draftId;
    @NotNull
    private Map<String, Object> cardProps;
    private List<String> atUsers;
}
