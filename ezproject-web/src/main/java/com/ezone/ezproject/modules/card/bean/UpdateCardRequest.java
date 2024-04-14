package com.ezone.ezproject.modules.card.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateCardRequest {
    @NotNull
    private Map<String, Object> cardProps;
    private AtUsersChange atUsersChange;
}
