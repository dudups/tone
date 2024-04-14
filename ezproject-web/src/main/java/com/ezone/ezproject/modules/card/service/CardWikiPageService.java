package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.SortUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.CardWikiPageRel;
import com.ezone.ezproject.dal.entity.CardWikiPageRelExample;
import com.ezone.ezproject.dal.mapper.CardWikiPageRelMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.BatchBindRequest;
import com.ezone.ezproject.modules.card.bean.BatchBindResponse;
import com.ezone.ezproject.modules.card.bean.query.BindType;
import com.ezone.ezproject.modules.cli.EzWikiCliService;
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
public class CardWikiPageService {
    private CardWikiPageRelMapper cardWikiPageRelMapper;

    private UserService userService;

    private EzWikiCliService ezWikiCliService;

    private TransactionHelper transactionHelper;

    private SqlSessionTemplate sqlSessionTemplate;

    public CardWikiPageRel bind(String user, Long cardId, Long pageSpaceId, Long pageId, boolean checkWikiPage) {
        CardWikiPageRel cardWikiPageRel = select(cardId, pageId);
        if (cardWikiPageRel != null) {
            return cardWikiPageRel;
        }
        if (checkWikiPage) {
            List<Long> pageIds = ezWikiCliService.checkAndFilterWikiPage(user, pageSpaceId, Arrays.asList(pageId));
            if (CollectionUtils.isEmpty(pageIds)) {
                throw CodedException.NOT_FOUND;
            }
        }
        cardWikiPageRel = CardWikiPageRel.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .wikiSpaceId(pageSpaceId)
                .pageId(pageId)
                .createTime(new Date())
                .createUser(user)
                .build();
        cardWikiPageRelMapper.insert(cardWikiPageRel);
        return cardWikiPageRel;
    }


    public CardWikiPageRel bind(Long cardId, Long pageSpaceId, Long pageId) {
        return bind(userService.currentUserName(), cardId, pageSpaceId, pageId, true);
    }

    public BatchBindResponse batchBind(Long cardId, BatchBindRequest request) {
        return batchBind(userService.currentUserName(), cardId, request, true);
    }

    public List<CardWikiPageRel> selectByCardId(Long cardId) {
        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        return cardWikiPageRelMapper.selectByExample(example);
    }

    public RelatesBean<CardWikiPageRel> selectRelatesBean(Long cardId) {
        List<CardWikiPageRel> relates = selectByCardId(cardId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<CardWikiPageRel>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        return RelatesBean.<CardWikiPageRel>builder()
                .relates(relates)
                .refs(ezWikiCliService.listWikiPageAndSpaces(relates.stream().map(CardWikiPageRel::getPageId).collect(Collectors.toList())))
                .build();
    }

    public CardWikiPageRel select(Long cardId, Long pageId) {
        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andPageIdEqualTo(pageId);
        List<CardWikiPageRel> rels = cardWikiPageRelMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public List<CardWikiPageRel> select(Long cardId, List<Long> pageIds) {
        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andPageIdIn(pageIds);
        return cardWikiPageRelMapper.selectByExample(example);
    }

    public void unBind(Long cardId, Long pageId) {
        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andPageIdEqualTo(pageId);
        cardWikiPageRelMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        cardWikiPageRelMapper.deleteByPrimaryKey(id);
    }

    public void deleteByCardId(Long cardId) {
        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        cardWikiPageRelMapper.deleteByExample(example);
    }

    public void deleteByCardIds(List<Long> cardIds) {
        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdIn(cardIds);
        cardWikiPageRelMapper.deleteByExample(example);
    }

    public void updateBind(Long cardId, Long spaceId, List<Long> pageRelIds) {
        String user = userService.currentUserName();
        final List<Long> toPageIds = CollectionUtils.isEmpty(pageRelIds)
                ? ListUtils.EMPTY_LIST
                : ezWikiCliService.checkAndFilterWikiPage(user, spaceId, pageRelIds);

        CardWikiPageRelExample example = new CardWikiPageRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andWikiSpaceIdEqualTo(spaceId);
        List<CardWikiPageRel> fromRels = cardWikiPageRelMapper.selectByExample(example);
        List<Long> bindedPageIds = new ArrayList<>();
        fromRels.stream().forEach(rel -> {
            if (toPageIds.contains(rel.getPageId())) {
                bindedPageIds.add(rel.getPageId());
            } else {
                cardWikiPageRelMapper.deleteByPrimaryKey(rel.getId());
            }
        });
        toPageIds.stream()
                .filter(pageId -> !bindedPageIds.contains(pageId))
                .forEach(pageId -> cardWikiPageRelMapper.insert(CardWikiPageRel.builder()
                        .id(IdUtil.generateId())
                        .cardId(cardId)
                        .pageId(pageId)
                        .wikiSpaceId(spaceId)
                        .createTime(new Date())
                        .createUser(user)
                        .build()));
    }

    private BatchBindResponse batchBind(String user, Long cardId, BatchBindRequest request, boolean checkWikiPage) {
        List<BatchBindRequest.RelateTarget> relateTargets = request.getRelateTargets();
        List<Long> pageIds = relateTargets.stream().map(BatchBindRequest.RelateTarget::getRelateTargetId).collect(Collectors.toList());
        List<CardWikiPageRel> cardWikiPageRels = select(cardId, pageIds);

        relateTargets = removeRelated(relateTargets, cardWikiPageRels);

        if (CollectionUtils.size(relateTargets) == 0) {
            return BatchBindResponse.builder().successAdd(selectRelatesBean(cardId)).errorMsg("").build();
        }

        String errorMsg;
        Map<Long, List<BatchBindRequest.RelateTarget>> spacePageMap = relateTargets.stream().collect(Collectors.groupingBy(BatchBindRequest.RelateTarget::getSpaceId));
        if (checkWikiPage) {
            errorMsg = checkAndRemove(user, spacePageMap);
        } else {
            errorMsg = "";
        }

        List<CardWikiPageRel> addPageRels = new ArrayList<>();
        spacePageMap.forEach((spaceId, targetList) -> targetList.forEach(
                relateTarget -> addPageRels.add(CardWikiPageRel.builder()
                        .id(IdUtil.generateId())
                        .cardId(cardId)
                        .wikiSpaceId(spaceId)
                        .pageId(relateTarget.getRelateTargetId())
                        .createTime(new Date())
                        .createUser(user)
                        .build())
        ));

        List<CardWikiPageRel> sortedAddPageRels = SortUtil.sortByIds(pageIds, addPageRels, CardWikiPageRel::getPageId);
        transactionHelper.runWithRequiresNew(() -> batchInsert(sortedAddPageRels));
        return BatchBindResponse.builder().bindType(BindType.WIKI).successAdd(selectRelatesBean(cardId)).errorMsg(errorMsg).build();
    }

    private String checkAndRemove(String user, Map<Long, List<BatchBindRequest.RelateTarget>> spacePageMap) {
        List<String> notFindWikiTitles = new ArrayList<>();
        Map<Long, List<Long>> toCheckSpaceWikiIds = new HashMap<>();
        spacePageMap.forEach((spaceId, relates) -> {
            toCheckSpaceWikiIds.putIfAbsent(spaceId, relates.stream().map(BatchBindRequest.RelateTarget::getRelateTargetId).collect(Collectors.toList()));
        });
        StringBuilder errorMsg = new StringBuilder();
        Map<Long, List<Long>> checkedPageIds = ezWikiCliService.checkAndFilterWikiPage(user, toCheckSpaceWikiIds);

        spacePageMap.forEach((spaceId, relateTargets) -> {
            List<Long> wikiIds = toCheckSpaceWikiIds.get(spaceId);
            List<Long> checkedWikiIds = checkedPageIds.get(spaceId);
            if (wikiIds == null) {
                checkedWikiIds = new ArrayList<>();
            }
            if (checkedWikiIds == null) {
                checkedWikiIds = new ArrayList<>();
            }
            Collection<Long> errorIds = CollectionUtils.subtract(wikiIds, checkedWikiIds);
            for (Long notFindId : errorIds) {
                Iterator<BatchBindRequest.RelateTarget> iterator = relateTargets.iterator();
                while (iterator.hasNext()) {
                    BatchBindRequest.RelateTarget relate = iterator.next();
                    if (relate.getRelateTargetId().equals(notFindId)) {
                        notFindWikiTitles.add(relate.getTitle());
                        iterator.remove();
                    }
                }
            }
        });
        if (CollectionUtils.isNotEmpty(notFindWikiTitles)) {
            errorMsg.append("对以下wiki没有权限：").append(StringUtils.join(notFindWikiTitles, "； "));
        }
        return errorMsg.toString();
    }

    private List<BatchBindRequest.RelateTarget> removeRelated(List<BatchBindRequest.RelateTarget> relateTargets, List<CardWikiPageRel> relatedTargets) {
        List<BatchBindRequest.RelateTarget> subRelateTargets = new ArrayList<>();
        for (BatchBindRequest.RelateTarget relateTarget : relateTargets) {
            boolean isRelated = false;
            for (CardWikiPageRel relatedTarget : relatedTargets) {
                if (relatedTarget.getPageId().equals(relateTarget.getRelateTargetId()) && relatedTarget.getWikiSpaceId().equals(relateTarget.getSpaceId())) {
                    isRelated = true;
                }
            }
            if (!isRelated) {
                subRelateTargets.add(relateTarget);
            }
        }
        return subRelateTargets;
    }

    public void batchInsert(List<CardWikiPageRel> addPageRels) {
        try (SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            CardWikiPageRelMapper mapper = sqlSession.getMapper(CardWikiPageRelMapper.class);
            addPageRels.forEach(mapper::insert);
            sqlSession.commit();
            sqlSession.flushStatements();
        }
    }
}
