package com.ezone.ezproject.modules.comment.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.Size;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateCommentRequest {
    @Size(max = 4000, message = "评论内容最多4000个字")
    private String comment;
    private List<String> atUsers;
}
