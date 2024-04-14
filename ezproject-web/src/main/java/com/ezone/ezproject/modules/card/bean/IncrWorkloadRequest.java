package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IncrWorkloadRequest {
    private BpmUserChoosesRequest bpmUserChoosesRequest;
    @NotEmpty
    private String owner;
    @NotNull
    private Date startTime;
    @NotNull
    private Date endTime;
    @Size(max = 200)
    private String description;

    public float calcIncrHours() {
        return (endTime.getTime() - startTime.getTime()) / 3600000F;
    }

    public void checkTime() {
        if (startTime == null || endTime == null || startTime.after(endTime)) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "开始结束时间非法！");
        }
    }
}
