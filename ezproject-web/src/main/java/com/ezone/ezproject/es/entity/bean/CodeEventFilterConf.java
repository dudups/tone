package com.ezone.ezproject.es.entity.bean;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeEventFilterConf implements AutoStatusFlowEventFilterConf<String> {
    private BranchesFilterType branchesFilterType;
    @ApiModelProperty(value = "branchesFilterType为list时有效")
    private List<BranchFilter> branchFilters;

    @Override
    public boolean match(String branch) {
        if (BranchesFilterType.all.equals(this.branchesFilterType)) {
            return true;
        }
        if (CollectionUtils.isEmpty(branchFilters)) {
            return false;
        }
        return branchFilters.stream()
                .anyMatch(f -> {
                    if (BranchFilterType.precise.equals(f.type)) {
                        return StringUtils.equals(branch, f.branch);
                    } else {
                        return StringUtils.startsWith(branch, f.branch);
                    }
                });
    }

    public enum BranchesFilterType {
        all, list
    }

    @ApiModel
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchFilter {
        private BranchFilterType type;
        private String branch;
    }

    public enum BranchFilterType {
        precise, prefix
    }
}
