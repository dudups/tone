package com.ezone.ezproject.modules.card.bean;

import com.ezone.ezbase.iam.bean.enums.SystemType;
import com.ezone.ezproject.modules.card.bean.query.BindType;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BatchBindRequest {

    @ApiModelProperty(value = "绑定的资源类型", example = "")
    @NotNull
    private BindType bindType;

    @ApiModelProperty(value = "批量关联对象")
    @NotNull
    private List<RelateTarget> relateTargets;

    public Map<Long, List<RelateTarget>> spaceRefIdsMap() {
        Map<Long, List<RelateTarget>> result = new HashMap<>();
        for (RelateTarget relateTarget : getRelateTargets()) {
            Long spaceId = relateTarget.getSpaceId();
            List<RelateTarget> list = result.getOrDefault(spaceId, new ArrayList<>());
            list.add(relateTarget);
            result.put(spaceId, list);
        }
        return result;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class RelateTarget {
        private Long relateTargetId;
        private Long spaceId;
        private String title;
    }
}
