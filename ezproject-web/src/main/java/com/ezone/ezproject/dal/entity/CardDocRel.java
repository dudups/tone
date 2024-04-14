package com.ezone.ezproject.dal.entity;

import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Database Table Remarks:
 *   卡片关联doc
 *
 * This class was generated by MyBatis Generator.
 * This class corresponds to the database table card_doc_rel
 * @Author mybatis-code-generator
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardDocRel implements Serializable {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "卡片ID")
    private Long cardId;

    @ApiModelProperty(value = "doc ID")
    private Long docId;

    @ApiModelProperty(value = "添加人")
    private String createUser;

    @ApiModelProperty(value = "添加时间")
    private Date createTime;

    @ApiModelProperty(value = "doc space Id")
    private Long docSpaceId;

    private static final long serialVersionUID = 1L;
}