package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.SortUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.CardDocRel;
import com.ezone.ezproject.dal.entity.CardDocRelExample;
import com.ezone.ezproject.dal.mapper.CardDocRelMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.BatchBindRequest;
import com.ezone.ezproject.modules.card.bean.BatchBindResponse;
import com.ezone.ezproject.modules.card.bean.query.BindType;
import com.ezone.ezproject.modules.cli.EzDocCliService;
import com.ezone.ezproject.modules.common.TransactionHelper;
import com.ezone.ezproject.modules.project.bean.RelatesBean;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class CardDocService {
    private CardDocRelMapper cardDocRelMapper;

    private UserService userService;

    private EzDocCliService ezDocCliService;

    private TransactionHelper transactionHelper;

    private SqlSessionTemplate sqlSessionTemplate;

    public CardDocRel bind(String user, Long cardId, Long docSpaceId, Long docId, boolean checkDoc) {
        CardDocRel cardDocRel = select(cardId, docSpaceId, docId);
        if (cardDocRel != null) {
            return cardDocRel;
        }
        if (checkDoc) {
            List<Long> docIds = ezDocCliService.checkAndFilterDoc(user, docSpaceId, Arrays.asList(docId));
            if (CollectionUtils.isEmpty(docIds)) {
                throw CodedException.NOT_FOUND;
            }
        }
        cardDocRel = CardDocRel.builder()
                .id(IdUtil.generateId())
                .cardId(cardId)
                .docSpaceId(docSpaceId)
                .docId(docId)
                .createTime(new Date())
                .createUser(user)
                .build();
        cardDocRelMapper.insert(cardDocRel);
        return cardDocRel;
    }


    public CardDocRel bind(Long cardId, Long docSpaceId, Long docId) {
        return bind(userService.currentUserName(), cardId, docSpaceId, docId, true);
    }

    public BatchBindResponse batchBind(Long cardId, BatchBindRequest request) {
        return batchBind(userService.currentUserName(), cardId, request, true);
    }

    public List<CardDocRel> selectByCardId(Long cardId) {
        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        return cardDocRelMapper.selectByExample(example);
    }

    public RelatesBean<CardDocRel> selectRelatesBean(Long cardId) {
        List<CardDocRel> relates = selectByCardId(cardId);
        if (CollectionUtils.isEmpty(relates)) {
            return RelatesBean.<CardDocRel>builder()
                    .relates(ListUtils.EMPTY_LIST)
                    .build();
        }
        Map<Long, List<Long>> spaceDocIds = relates.stream().collect(Collectors.groupingBy(
                CardDocRel::getDocSpaceId,
                Collectors.mapping(CardDocRel::getDocId, Collectors.toList())));
        return RelatesBean.<CardDocRel>builder()
                .relates(relates)
                .refs(ezDocCliService.listDocAndSpaces(spaceDocIds))
                .build();
    }

    public CardDocRel select(Long cardId, Long docSpaceId, Long docId) {
        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andDocSpaceIdEqualTo(docSpaceId).andDocIdEqualTo(docId);
        List<CardDocRel> rels = cardDocRelMapper.selectByExample(example);
        return CollectionUtils.isEmpty(rels) ? null : rels.get(0);
    }

    public List<CardDocRel> select(Long cardId, List<Long> docSpaceIds, List<Long> docIds) {
        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andDocSpaceIdIn(docSpaceIds).andDocIdIn(docIds);
        return cardDocRelMapper.selectByExample(example);
    }

    public void unBind(Long cardId, Long docSpaceId, Long docId) {
        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andDocSpaceIdEqualTo(docSpaceId).andDocIdEqualTo(docId);
        cardDocRelMapper.deleteByExample(example);
    }

    public void delete(Long id) {
        cardDocRelMapper.deleteByPrimaryKey(id);
    }

    public void deleteByCardId(Long cardId) {
        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdEqualTo(cardId);
        cardDocRelMapper.deleteByExample(example);
    }

    public void deleteByCardIds(List<Long> cardIds) {
        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdIn(cardIds);
        cardDocRelMapper.deleteByExample(example);
    }

    public void updateBind(Long cardId, Long spaceId, List<Long> docIds) {
        String user = userService.currentUserName();
        final List<Long> toDocIds = CollectionUtils.isEmpty(docIds)
                ? ListUtils.EMPTY_LIST
                : ezDocCliService.checkAndFilterDoc(user, spaceId, docIds);

        CardDocRelExample example = new CardDocRelExample();
        example.createCriteria().andCardIdEqualTo(cardId).andDocSpaceIdEqualTo(spaceId);
        List<CardDocRel> fromRels = cardDocRelMapper.selectByExample(example);
        List<Long> bindedDocIds = new ArrayList<>();
        fromRels.stream().forEach(rel -> {
            if (toDocIds.contains(rel.getDocId())) {
                bindedDocIds.add(rel.getDocId());
            } else {
                cardDocRelMapper.deleteByPrimaryKey(rel.getId());
            }
        });
        toDocIds.stream()
                .filter(docId -> !bindedDocIds.contains(docId))
                .forEach(docId -> cardDocRelMapper.insert(CardDocRel.builder()
                        .id(IdUtil.generateId())
                        .cardId(cardId)
                        .docId(docId)
                        .docSpaceId(spaceId)
                        .createTime(new Date())
                        .createUser(user)
                        .build()));
    }

    @SuppressWarnings("unchecked")
    public BatchBindResponse batchBind(String user, Long cardId, BatchBindRequest request, boolean checkDoc) {
        List<BatchBindRequest.RelateTarget> relateTargets = request.getRelateTargets();
        List<Long> docSpaceIds = relateTargets.stream().map(BatchBindRequest.RelateTarget::getSpaceId).distinct().collect(Collectors.toList());
        List<Long> docIds = relateTargets.stream().map(BatchBindRequest.RelateTarget::getRelateTargetId).collect(Collectors.toList());
        List<CardDocRel> relatedTargets = select(cardId, docSpaceIds, docIds);

        relateTargets = removeRelated(relateTargets, relatedTargets);

        if (CollectionUtils.size(relateTargets) == 0) {
            return BatchBindResponse.builder().bindType(BindType.DOC).successAdd(selectRelatesBean(cardId)).errorMsg("").build();
        }

        String errorMsg;
        Map<Long, List<BatchBindRequest.RelateTarget>> relateTargetMap = relateTargets.stream().collect(Collectors.groupingBy(BatchBindRequest.RelateTarget::getSpaceId));
        if (checkDoc) {
            errorMsg = checkAndRemove(user, relateTargetMap);
        } else {
            errorMsg = "";
        }

        List<CardDocRel> addPageRelates = new ArrayList<>();
        relateTargetMap.forEach((spaceId, targetList) -> targetList.forEach(
                relateTarget -> addPageRelates.add(CardDocRel.builder()
                        .id(IdUtil.generateId())
                        .cardId(cardId)
                        .docSpaceId(spaceId)
                        .docId(relateTarget.getRelateTargetId())
                        .createTime(new Date())
                        .createUser(user)
                        .build())
        ));

        List<CardDocRel> sortedAddPageRels = SortUtil.sortByIds(docIds, addPageRelates, CardDocRel::getDocId);
        transactionHelper.runWithRequiresNew(() ->
                batchInsert(sortedAddPageRels));
        return BatchBindResponse.builder().successAdd(selectRelatesBean(cardId)).errorMsg(errorMsg).build();
    }

    private String checkAndRemove(String user, Map<Long, List<BatchBindRequest.RelateTarget>> spaceDocMap) {
        List<String> notFindDocTitles = new ArrayList<>();
        Map<Long, List<Long>> toCheckSpaceDocIds = new HashMap<>();
        spaceDocMap.forEach((spaceId, relates) -> {
            toCheckSpaceDocIds.putIfAbsent(spaceId, relates.stream().map(BatchBindRequest.RelateTarget::getRelateTargetId).collect(Collectors.toList()));
        });
        StringBuilder errorMsg = new StringBuilder();
        Map<Long, List<Long>> checkedPageIds = ezDocCliService.checkAndFilterDoc(user, toCheckSpaceDocIds);
        spaceDocMap.forEach((spaceId, relateTargets) -> {
            List<Long> docIds = toCheckSpaceDocIds.get(spaceId);
            List<Long> checkedWikiIds = checkedPageIds.get(spaceId);
            if (docIds == null) {
                checkedWikiIds = new ArrayList<>();
            }
            if (checkedWikiIds == null) {
                checkedWikiIds = new ArrayList<>();
            }
            Collection<Long> errorIds = CollectionUtils.subtract(docIds, checkedWikiIds);
            for (Long notFindId : errorIds) {
                Iterator<BatchBindRequest.RelateTarget> iterator = relateTargets.iterator();
                while (iterator.hasNext()) {
                    BatchBindRequest.RelateTarget relate = iterator.next();
                    if (relate.getRelateTargetId().equals(notFindId)) {
                        notFindDocTitles.add(relate.getTitle());
                        iterator.remove();
                    }
                }
            }
        });
        if (CollectionUtils.isNotEmpty(notFindDocTitles)) {
            errorMsg.append("对以下文档没有权限：").append(StringUtils.join(notFindDocTitles, "； "));
        }
        return errorMsg.toString();
    }

    private List<BatchBindRequest.RelateTarget> removeRelated(List<BatchBindRequest.RelateTarget> relateTargets, List<CardDocRel> relatedTargets) {
        if (CollectionUtils.isEmpty(relatedTargets)) {
            return relateTargets;
        }
        List<BatchBindRequest.RelateTarget> subRelateTargets = new ArrayList<>();
        for (BatchBindRequest.RelateTarget relateTarget : relateTargets) {
            boolean isRelated = false;
            for (CardDocRel relatedTarget : relatedTargets) {
                if (relatedTarget.getDocId().equals(relateTarget.getRelateTargetId()) && relatedTarget.getDocSpaceId().equals(relateTarget.getSpaceId())) {
                    isRelated = true;
                }
            }
            if (!isRelated) {
                subRelateTargets.add(relateTarget);
            }
        }
        return subRelateTargets;
    }

    public void batchInsert(List<CardDocRel> addDocRels) {
        try (SqlSession sqlSession = sqlSessionTemplate.getSqlSessionFactory().openSession(ExecutorType.BATCH, false)) {
            CardDocRelMapper mapper = sqlSession.getMapper(CardDocRelMapper.class);
            addDocRels.forEach(mapper::insert);
            sqlSession.commit();
            sqlSession.flushStatements();
        }
    }

}
