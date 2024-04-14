package com.ezone.ezproject.modules.project.bean;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RelatesBean<R> {
    @ApiModelProperty(value = "关联关系")
    private List<R> relates;

    @ApiModelProperty(value = "其它系统返回的关联对象，一般为Map或List")
    private Object refs;
}
