package com.ezone.ezproject.modules.card.event.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddAttachmentEventMsg implements EventMsg {
    private String fileName;
}
