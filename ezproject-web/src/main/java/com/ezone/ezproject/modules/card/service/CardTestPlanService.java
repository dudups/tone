package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.SortUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.CardTestPlan;
import com.ezone.ezproject.dal.entity.CardTestPlanExample;
import com.ezone.ezproject.dal.mapper.CardTestPlanMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.BatchBindRequest;
import com.ezone.ezproject.modules.card.bean.BatchBindResponse;
import com.ezone.ezproject.modules.card.bean.query.BindType;
import com.ezone.ezproject.modules.cli.EzTestCliService;
import com.ezone.ezproject.modules.common.TransactionHelper;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardTestPlanService {
    private CardTestPlanMapper cardPlanMapper;

    private UserService userService;

    private EzTestCliService ezTestCliService;

    private TransactionHelper transactionHelper;

    private SqlSessionTemplate sqlSessionTemplate;

    public CardTestPlan bind(String user, Long cardId, Long spaceId, Long testPlanId, boolean checkTestPlan) {
        CardTestPlan cardPlan = select(cardId, testPlanId);
        if (cardPlan != null) {
            return cardPlan;
        }
        if (checkTestPlan) {
            List<Long> testPlanIds = ezTestCliService.checkAndFilterPlan(user, spaceId, Arrays.asList(spaceId));
            if (CollectionUtils.isEmpty(testPlanIds)) {
                throw CodedException.NOT_FOUND;
            }
        }
        cardPlan = CardTestPlan.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .testPlanId(testPlanId)
                .testSpaceId(spaceId)
                .createTime(new Date())
                .createUser(user)
                .build();
        cardPlanMapper.insert(cardPlan);
        return cardPlan;
    }

    public CardTestPlan bind(Long cardId, Long spaceId, Long planId) {
        return bind(userService.currentUserName(), cardId, spaceId, planId, true);
    }

    public BatchBindResponse batchBind(Long cardId, BatchBindRequest request) {
        return batchBind(userService.currentUserName(), cardId, request, true);
    }

    public List<CardTestPlan> selectByCardId(Long cardId) {
        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        return cardPlanMapper.selectByExample(example);
    }

    public RelatesBean<CardTestPlan> selectRelatesBean(Long cardId) {
        List<CardTestPlan> relates = selectByCardId(cardId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<CardTestPlan>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<CardTestPlan>builder()
                .relates(relates)
                .refs(ezTestCliService.listPlanAndSpaces(relates.stream().map(CardTestPlan::getTestPlanId).collect(Collectors.toList())))
                .build();
    }

    public CardTestPlan select(Long cardId, Long planId) {
        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdEqualTo(cardId).andTestPlanIdEqualTo(planId);
        List<CardTestPlan> rels = cardPlanMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public List<CardTestPlan> select(Long cardId, List<Long> planId) {
        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdEqualTo(cardId).andTestPlanIdIn(planId);
        return cardPlanMapper.selectByExample(example);
    }

    public void unBind(Long cardId, Long planId) {
        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdEqualTo(cardId).andTestPlanIdEqualTo(planId);
        cardPlanMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        cardPlanMapper.deleteByPrimaryKey(id);
    }

    public void deleteByCardId(Long cardId) {
        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        cardPlanMapper.deleteByExample(example);
    }

    public void deleteByCardIds(List<Long> cardIds) {
        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdIn(cardIds);
        cardPlanMapper.deleteByExample(example);
    }

    public void updateBind(Long cardId, Long spaceId, List<Long> testPlanIds) {
        String user = userService.currentUserName();
        final List<Long> toPlanIds = CollectionUtils.isEmpty(testPlanIds)
                ? ListUtils.EMPTY_LIST
                : ezTestCliService.checkAndFilterPlan(user, spaceId, testPlanIds);

        CardTestPlanExample example = new CardTestPlanExample();
        example.createCriteria().andCardIdEqualTo(cardId).andTestSpaceIdEqualTo(spaceId);
        List<CardTestPlan> fromRels = cardPlanMapper.selectByExample(example);
        List<Long> bindedPlanIds = new ArrayList<>();
        fromRels.stream().forEach(rel -> {
            if (toPlanIds.contains(rel.getTestPlanId())) {
                bindedPlanIds.add(rel.getTestPlanId());
            } else {
                cardPlanMapper.deleteByPrimaryKey(rel.getId());
            }
        });
        toPlanIds.stream()
                .filter(planId -> !bindedPlanIds.contains(planId))
                .forEach(planId -> cardPlanMapper.insert(CardTestPlan.builder()
                        .id(IdUtil.generateId())
                        .cardId(cardId)
                        .testPlanId(planId)
                        .testSpaceId(spaceId)
                        .createTime(new Date())
                        .createUser(user)
                        .build()));
    }

    public BatchBindResponse batchBind(String user, Long cardId, BatchBindRequest request, boolean checkTestPlan) {
        List<BatchBindRequest.RelateTarget> relateTargets = request.getRelateTargets().stream().collect(Collectors.toList());
        List<Long> planIds = relateTargets.stream().map(BatchBindRequest.RelateTarget::getRelateTargetId).collect(Collectors.toList());
        List<CardTestPlan> relatedTargets = select(cardId, planIds);

        relateTargets = removeRelated(relateTargets, relatedTargets);
        if (CollectionUtils.size(relateTargets) == 0) {
            return BatchBindResponse.builder().successAdd(selectRelatesBean(cardId)).errorMsg("").build();
        }

        String errorMsg;
        Map<Long, List<BatchBindRequest.RelateTarget>> spaceDocMap = relateTargets.stream().collect(Collectors.groupingBy(BatchBindRequest.RelateTarget::getSpaceId));
        if (checkTestPlan) {
            errorMsg = checkAndRemove(user, spaceDocMap);
        } else {
            errorMsg = "";
        }

        List<CardTestPlan> addCardTestPlans = new ArrayList<>();
        spaceDocMap.forEach((spaceId, targetList) -> targetList.forEach(
                relateTarget -> addCardTestPlans.add(CardTestPlan.builder()
                        .id(IdUtil.generateId())
                        .cardId(cardId)
                        .testPlanId(relateTarget.getRelateTargetId())
                        .testSpaceId(spaceId)
                        .createTime(new Date())
                        .createUser(user)
                        .build())
        ));

        List<CardTestPlan> sortedAddPageRels = SortUtil.sortByIds(planIds, addCardTestPlans, CardTestPlan::getTestPlanId);
        transactionHelper.runWithRequiresNew(() -> batchInsert(sortedAddPageRels));
        return BatchBindResponse.builder().bindType(BindType.TEST_PLAN).successAdd(selectRelatesBean(cardId)).errorMsg(errorMsg).build();
    }

    private String checkAndRemove(String user, Map<Long, List<BatchBindRequest.RelateTarget>> spaceDocMap) {
        List<String> notFindPlanTitles = new ArrayList<>();
        Map<Long, List<Long>> toCheckSpacePlanIds = new HashMap<>();
        spaceDocMap.forEach((spaceId, relates) -> {
            toCheckSpacePlanIds.putIfAbsent(spaceId, relates.stream().map(BatchBindRequest.RelateTarget::getRelateTargetId).collect(Collectors.toList()));
        });
        StringBuilder errorMsg = new StringBuilder();
        Map<Long, List<Long>> checkedSpacePlanIds = ezTestCliService.checkAndFilterPlan(user, toCheckSpacePlanIds);

        spaceDocMap.forEach((spaceId, relateTargets) -> {
            List<Long> planIds = toCheckSpacePlanIds.get(spaceId);
            List<Long> checkedPlanIds = checkedSpacePlanIds.get(spaceId);
            if (planIds == null) {
                planIds = new ArrayList<>();
            }
            if (checkedPlanIds == null) {
                checkedPlanIds = new ArrayList<>();
            }
            Collection<Long> errorIds = CollectionUtils.subtract(planIds, checkedPlanIds);
            for (Long notFindId : errorIds) {
                Iterator<BatchBindRequest.RelateTarget> iterator = relateTargets.iterator();
                while (iterator.hasNext()) {
                    BatchBindRequest.RelateTarget relate = iterator.next();
                    if (relate.getRelateTargetId().equals(notFindId)) {
                        notFindPlanTitles.add(relate.getTitle());
                        iterator.remove();
                    }
                }
            }
        });
        if (CollectionUtils.isNotEmpty(notFindPlanTitles)) {
            errorMsg.append("对以下测试执行没有权限：").append(StringUtils.join(notFindPlanTitles, "； "));
        }
        return errorMsg.toString();
    }

    private List<BatchBindRequest.RelateTarget> removeRelated(List<BatchBindRequest.RelateTarget> relateTargets, List<CardTestPlan> relatedTargets) {
        List<BatchBindRequest.RelateTarget> subRelateTargets = new ArrayList<>();
        for (BatchBindRequest.RelateTarget relateTarget : relateTargets) {
            boolean isRelated = false;
            for (CardTestPlan relatedTarget : relatedTargets) {
                if (relatedTarget.getTestPlanId().equals(relateTarget.getRelateTargetId()) && relatedTarget.getTestSpaceId().equals(relateTarget.getSpaceId())) {
                    isRelated = true;
                    break;
                }
            }
            if (!isRelated) {
                subRelateTargets.add(relateTarget);
            }
        }
        return subRelateTargets;
    }

    public void batchInsert(List<CardTestPlan> cardTestPlan) {
        try (SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            CardTestPlanMapper mapper = sqlSession.getMapper(CardTestPlanMapper.class);
            cardTestPlan.forEach(mapper::insert);
            sqlSession.commit();
            sqlSession.flushStatements();
        }
    }

}
