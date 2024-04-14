package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.dal.entity.Card;
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
public class CreateCardAndBindResponse {
    private Card card;
    List<BatchBindResponse> binds;
}
