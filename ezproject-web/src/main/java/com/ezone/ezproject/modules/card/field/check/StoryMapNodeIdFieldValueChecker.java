package com.ezone.ezproject.modules.card.field.check;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.function.Function;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class StoryMapNodeIdFieldValueChecker implements IFieldValueChecker {
    private Long projectId;
    private Function<Long, StoryMapNode> findStoryMapNodeById;

    @Override
    public void check(Object nodeId) throws CodedException {
        Long id = FieldUtil.toLong(nodeId);
        if (id != null && id > 0) {
            StoryMapNode node = findStoryMapNodeById.apply(FieldUtil.toLong(nodeId));
            if (node == null) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "绑定的二级分类不存在!");
            } else if (node.getParentId().equals(0L)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能绑定一级分类!");
            } else if (!node.getProjectId().equals(projectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能跨项目绑定二级分类!");
            }
        }
    }
}
