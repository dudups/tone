package com.ezone.ezproject.modules.card.bean;

import com.ezone.devops.ezcode.base.enums.CardRelateType;
import io.swagger.annotations.ApiModelProperty;
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
public class BindBranchRequest {
    @ApiModelProperty(value = "项目ID")
    private Long projectId;
    @ApiModelProperty(value = "代码库ID")
    private Long repoId;
    @ApiModelProperty(value = "资源类型，如评审、推送、分支等")
    private String relateKey;
    @ApiModelProperty(value = "分支类型：BRANCH,TAG,PUSH,REVIEW,COMMIT")
    private CardRelateType refType;
}
