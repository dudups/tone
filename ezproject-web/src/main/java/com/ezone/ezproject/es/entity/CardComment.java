package com.ezone.ezproject.es.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CardComment {
    private Long id;
    private Long cardId;
    @ApiModelProperty(value = "针对L1级是直接子节点排序序号，针对L2及其以下，对应L1的所有子孙节点排序序号")
    private Long seqNum;
    @ApiModelProperty(value = "仅针对L1，其所有子孙节点排序，当前最大序号")
    private long maxSeqNum;
    @ApiModelProperty(value = "仅针对L2及其以下，其祖先L1的ID")
    private long ancestorId;
    @ApiModelProperty(value = "仅针对L2及其以下，其父评论的ID")
    private long parentId;
    @ApiModelProperty(value = "仅针对L2及其以下，其父评论的序号")
    private Long parentSeqNum;
    private String user;
    private String comment;
    private Date createTime;
    private Date lastModifyTime;
    private boolean deleted;

    private List<String> atUsers;

    public static final String CARD_ID = "cardId";
    public static final String USER = "user";
    public static final String SEQ_NUM = "seqNum";
    public static final String MAX_SEQ_NUM = "maxSeqNum";
    public static final String ANCESTOR_ID = "ancestorId";
}
