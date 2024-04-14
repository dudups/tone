package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.modules.cli.bean.CreateBpmFlow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BpmUserChoosesRequest {
    @NotNull
    CreateBpmFlow.ApproverChooseRequest[] userChooses;
}
