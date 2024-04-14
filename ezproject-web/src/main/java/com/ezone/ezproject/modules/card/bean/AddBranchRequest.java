package com.ezone.ezproject.modules.card.bean;

import com.ezone.devops.ezcode.sdk.bean.enums.RefType;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddBranchRequest {
    @ApiModelProperty(value = "项目ID")
    private Long projectId;
    @ApiModelProperty(value = "代码库ID")
    private Long repoId;
    @ApiModelProperty(value = "创建的分支名称")
    @Size(max = 255)
    private String name;
    @ApiModelProperty(value = "分支类型：BRANCH, TAG, COMMIT")
    private RefType refType;
    @ApiModelProperty(value = "基点引用名称，最长 255 字符")
    @Size(max = 255)
    private String baseRefName;
    @ApiModelProperty(value = "分支备注，最长 255 字符")
    @Size(max = 255)
    private String note;
}
