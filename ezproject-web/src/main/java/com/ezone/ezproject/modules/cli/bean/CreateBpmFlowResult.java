package com.ezone.ezproject.modules.cli.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBpmFlowResult {
    private Long id;
    private String status;

    public boolean isProcessing() {
        return "PROCESSING".equals(status);
    }

    /**
     * 通过
     * @return
     */
    public boolean isFinished() {
        return "FINISHED".equals(status);
    }
}
