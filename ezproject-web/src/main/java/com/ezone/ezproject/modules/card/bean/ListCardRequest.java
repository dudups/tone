package com.ezone.ezproject.modules.card.bean;

import com.ezone.devops.ezcode.sdk.bean.enums.RefType;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.Size;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ListCardRequest {
    @ApiModelProperty(value = "卡片ids", example = "1")
    List<Long> cardIds;
    @ApiModelProperty(value = "卡片字段", example = "title")
    String[] fields;
    @ApiModelProperty(value = "是否包含已经删除的卡片")
    boolean excludeDeleted;
}
