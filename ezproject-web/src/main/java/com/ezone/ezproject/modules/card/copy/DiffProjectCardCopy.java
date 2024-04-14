package com.ezone.ezproject.modules.card.copy;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.project.bean.CheckSchemaResult;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Slf4j
public class DiffProjectCardCopy extends AbstractCardCopy {
    private Project fromProject;
    private CheckSchemaResult checkSchemaResult;
    private ProjectCardSchema toSchema;

    @Override
    public Project fromProject() {
        return fromProject;
    }

    @Override
    protected Card newCard(Card card, Long seqNum, String rank) {
        return Card.builder()
                .id(IdUtil.generateId())
                .projectId(targetProject.getId())
                .projectKey(targetProject.getKey())
                .seqNum(seqNum)
                .planId(targetPlanId)
                .parentId(0L)
                .ancestorId(0L)
                .rank(rank)
                .companyId(targetProject.getCompanyId())
                .deleted(false)
                .maxCommentSeqNum(0L)
                .storyMapNodeId(0L)
                .latestEventId(0L)
                .build();
    }

    @Override
    protected Map<String, Object> newCardDetail(Map<String, Object> cardDetail, Card card) {
        Map<String, Object> newCardDetail = new HashMap<>();
        String type = String.valueOf(cardDetail.get("type"));
        CheckSchemaResult.CheckCardResult checkCardResult = checkSchemaResult.getCheckCardResults().get(type);
        for (Map.Entry<String, Object> entry : cardDetail.entrySet()) {
            if (SysFieldOpLimit.canOp(entry.getKey(), SysFieldOpLimit.Op.CREATE)) {
                // 字段：对不兼容的删除；对同名但key不同的兼容字段改为对应的新key；否则保留不动；
                if (checkCardResult.getIncompatibleFields().contains(entry.getKey()) || checkCardResult.getDisabledFields().contains(entry.getKey())) {
                    // ignore
                } else if (checkSchemaResult.getCompatibleFields().containsKey(entry.getKey())) {
                    newCardDetail.put(checkSchemaResult.getCompatibleFields().get(entry.getKey()), entry.getValue());
                } else {
                    newCardDetail.put(entry.getKey(), entry.getValue());
                }
            }
        }
        // 状态：对不兼容状态值改为默认状态值；对同名不同标识值的状态改为对应的状态标识值；否则保留不动；
        String status = String.valueOf(cardDetail.get("status"));
        String toStatus = status;
        if (checkCardResult.getIncompatibleStatuses().contains(status) || checkCardResult.getDisabledStatuses().contains(status)) {
            toStatus = checkCardResult.getDefaultStatus();
        } else if (checkSchemaResult.getCompatibleStatuses().containsKey(status)) {
            toStatus = checkSchemaResult.getCompatibleStatuses().get(status);
        }
        newCardDetail.put("status", toStatus);
        // 新卡片自动初始化字段
        CardHelper.setCardCreatedProps(newCardDetail, opContext, card);
        return newCardDetail;
    }

}
