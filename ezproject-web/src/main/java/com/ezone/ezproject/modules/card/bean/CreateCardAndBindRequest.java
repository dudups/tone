package com.ezone.ezproject.modules.card.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateCardAndBindRequest {
    private Map<String, Object> card;
    List<BatchBindRequest> binds;
}
