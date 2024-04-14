package com.ezone.ezproject.modules.card.service;

import com.ezone.devops.ezcode.base.util.Md5Util;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardToken;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectDomain;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.dal.mapper.ProjectDomainMapper;
import com.ezone.ezproject.es.dao.CardCommentDao;
import com.ezone.ezproject.es.entity.CardComment;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.bean.FieldChange;
import com.ezone.ezproject.modules.card.rank.DictRanker;
import com.ezone.ezproject.modules.card.rank.RankLocation;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.common.LockHelper;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.klock.lock.Lock;
import org.springframework.boot.autoconfigure.klock.lock.LockFactory;
import org.springframework.boot.autoconfigure.klock.model.LockInfo;
import org.springframework.boot.autoconfigure.klock.model.LockType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardHelper {
    public static final DictRanker RANKER = DictRanker.DEFAULT;

    public static final String START_RANK = "00001";

    public static final String PROJECT_FIRST_CARD_DEFAULT_RANK = "00010";

    public static final int RANK_LENGTH = START_RANK.length();

    private ProjectDomainMapper projectDomainMapper;

    private CardMapper cardMapper;

    private CardCommentDao cardCommentDao;

    private LockFactory lockFactory;

    private CardQueryService cardQueryService;

    private PlanQueryService planQueryService;

    private LockHelper lockHelper;

    public Long getMaxSeqNum(Long projectId) {
        return projectDomainMapper.selectByPrimaryKey(projectId).getMaxSeqNum();
    }

    public String getMaxRank(Long projectId) {
        return projectDomainMapper.selectByPrimaryKey(projectId).getMaxRank();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long seqNum(Long projectId) {
        return lockHelper.lockRun(lockSeqNum(projectId), () -> {
            Long maxSeqNum = getMaxSeqNum(projectId);
            projectDomainMapper.updateByPrimaryKeySelective(ProjectDomain.builder().id(projectId).maxSeqNum(maxSeqNum + 1).build());
            return maxSeqNum + 1;
        });
    }

    public Long commentSeqNum(Long cardId) {
        return lockHelper.lockRun(lockCardCommentSeqNum(cardId), () -> {
            Card card = cardMapper.selectByPrimaryKey(cardId);
            card.setMaxCommentSeqNum(card.getMaxCommentSeqNum() + 1);
            cardMapper.updateByPrimaryKey(card);
            return card.getMaxCommentSeqNum();
        });
    }

    public Long descendantCommentSeqNum(Long ancestorCommentId) {
        return lockHelper.lockRun(lockAncestorCommentSeqNum(ancestorCommentId), () -> {
            try {
                CardComment ancestorComment = cardCommentDao.find(ancestorCommentId, CardComment.MAX_SEQ_NUM);
                cardCommentDao.updateField(ancestorCommentId, CardComment.MAX_SEQ_NUM, ancestorComment.getMaxSeqNum() + 1);
                return ancestorComment.getMaxSeqNum() + 1;
            } catch (Exception e) {
                log.error("Gen commentParentSeqNum exception!", e);
                throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
            }
        });
    }

    /**
     * @param projectId
     * @return first seq num
     */
    public Long seqNums(Long projectId, int num) {
        return lockHelper.lockRun(lockSeqNum(projectId), () -> {
            Long maxSeqNum = getMaxSeqNum(projectId);
            projectDomainMapper.updateByPrimaryKeySelective(ProjectDomain.builder().id(projectId).maxSeqNum(maxSeqNum + num).build());
            return maxSeqNum + 1;
        });
    }

    public List<String> ranks(Long projectId, String referenceRank, RankLocation location, int num) {
        String start;
        String end;
        switch (location) {
            case HIGHER:
                start = referenceRank;
                end = higherRank(projectId, referenceRank);
                break;
            case LOWER:
            default:
                end = referenceRank;
                start = lowerRank(projectId, referenceRank);
        }
        return RANKER.ranks(start, end, num);
    }

    public String nextRank(Long projectId) {
        return lockHelper.lockRun(lockRank(projectId), () -> {
            String maxRank = getMaxRank(projectId);
            if (PROJECT_FIRST_CARD_DEFAULT_RANK.compareTo(maxRank) > 0) {
                maxRank = PROJECT_FIRST_CARD_DEFAULT_RANK;
            }
            ProjectDomain projectDomain = ProjectDomain.builder().id(projectId).maxRank(RANKER.next(maxRank, RANK_LENGTH)).build();
            projectDomainMapper.updateByPrimaryKeySelective(projectDomain);
            return projectDomain.getMaxRank();
        });
    }

    /**
     * @param projectId
     * @return 升序
     */
    public List<String> nextRanks(Long projectId, int num) {
        return lockHelper.lockRun(lockRank(projectId), () -> {
            String maxRank = getMaxRank(projectId);
            if (PROJECT_FIRST_CARD_DEFAULT_RANK.compareTo(maxRank) > 0) {
                maxRank = PROJECT_FIRST_CARD_DEFAULT_RANK;
            }
            List<String> ranks = RANKER.nextRanks(maxRank, RANK_LENGTH, num);
            ProjectDomain projectDomain = ProjectDomain.builder().id(projectId).maxRank(ranks.get(ranks.size() - 1)).build();
            projectDomainMapper.updateByPrimaryKeySelective(projectDomain);
            return ranks;
        });
    }

    @Deprecated
    public void processIsEnd4OldCard(Map<String, Object> fromCardDetail, Map<String, Object> toCardDetail, ProjectCardSchema schema) {
        Long oldLastEndTime = FieldUtil.getLastEndTime(fromCardDetail);
        boolean oldCalcIsEnd = FieldUtil.getCalcIsEnd(fromCardDetail);
        String type = FieldUtil.toString(toCardDetail.get(CardField.TYPE));
        String toStatus = FieldUtil.toString(toCardDetail.get(CardField.STATUS));

        fillCardDetailEndStatus(type, toStatus, oldLastEndTime, oldCalcIsEnd, toCardDetail, schema);
    }

    @Deprecated
    public Map<String, Object> genEndStatus4OldCard(Map<String, Object> fromCardDetail, String cardType, String toStatus, ProjectCardSchema schema) {
        Long oldLastEndTime = FieldUtil.getLastEndTime(fromCardDetail);
        boolean oldCalcIsEnd = FieldUtil.getCalcIsEnd(fromCardDetail);

        Map<String, Object> toCardDetail = new HashMap<>();

        // toCardDetail其实是增量部分属性，先workaround计算LAST_END_DELAY同时又确保相对原逻辑无任何副作用；
        // 后续需要重构，通过依赖分析机制加载受影响字段+依赖扩散机制修改冗余字段
        toCardDetail.put(CardField.END_DATE, fromCardDetail.get(CardField.END_DATE));
        fillCardDetailEndStatus(cardType, toStatus, oldLastEndTime, oldCalcIsEnd, toCardDetail, schema);
        toCardDetail.remove(CardField.END_DATE);

        return toCardDetail;
    }

    @Deprecated
    private void fillCardDetailEndStatus(String cardType, String toStatus, Long oldLastEndTime, boolean oldCalcIsEnd, Map<String, Object> toCardDetail, ProjectCardSchema schema) {
        boolean calcIsEnd = schema.isEndStatus(cardType, toStatus);
        Date now = new Date();
        if (oldLastEndTime == 0 && calcIsEnd) {
            toCardDetail.put(CardField.LAST_END_TIME, now.getTime());
        }
        if ((!oldCalcIsEnd) && calcIsEnd) {
            toCardDetail.put(CardField.LAST_END_TIME, now.getTime());
        }
        toCardDetail.put(CardField.CALC_IS_END, calcIsEnd);
        toCardDetail.put(CardField.LAST_END_DELAY, FieldUtil.calcLastEndDelay(toCardDetail));
    }

    @Deprecated
    public void processEndStatus4NewCard(Map<String, Object> cardDetail, String cardType, String status, ProjectCardSchema schema) {
        boolean endStatus = schema.isEndStatus(cardType, status);
        Date now = new Date();
        cardDetail.put(CardField.CALC_IS_END, endStatus);
        if (endStatus) {
            cardDetail.put(CardField.LAST_END_TIME, now.getTime());
        }
        cardDetail.put(CardField.LAST_END_DELAY, FieldUtil.calcLastEndDelay(cardDetail));
    }

    @Deprecated
    public void processEndStatus4NewCard(Map<String, Object> newCardDetail, ProjectCardSchema schema) {
        boolean endStatus = schema.isEndStatus(newCardDetail);
        Date now = new Date();
        newCardDetail.put(CardField.CALC_IS_END, endStatus);
        if (endStatus) {
            newCardDetail.put(CardField.LAST_END_TIME, now.getTime());
        }
        newCardDetail.put(CardField.LAST_END_DELAY, FieldUtil.calcLastEndDelay(newCardDetail));
    }

    public String cardAccessToken(Long accessFrom, CardToken cardToken) {
        return Md5Util.getMD5Code(String.format("%d%s", accessFrom, cardToken.getToken()));
    }

    @NotNull
    private String higherRank(Long projectId, String referenceRank) {
        String higherRank = null;
        Card higherRankCard = cardQueryService.selectHigherRankCard(projectId, referenceRank);
        if (null != higherRankCard) {
            higherRank = higherRankCard.getRank();
        }
        Plan higherRankPlan = planQueryService.selectHigherDeliverLineRankPlan(projectId, referenceRank);
        if (null != higherRankPlan) {
            if (null == higherRank || higherRank.compareTo(higherRankPlan.getDeliverLineRank()) > 0) {
                higherRank = higherRankPlan.getDeliverLineRank();
            }
        }
        if (null == higherRank) {
            higherRank = nextRank(projectId);
        }
        return higherRank;
    }

    @NotNull
    private String lowerRank(Long projectId, String referenceRank) {
        String lowerRank = null;
        Card lowerRankCard = cardQueryService.selectLowerRankCard(projectId, referenceRank);
        if (null != lowerRankCard) {
            lowerRank = lowerRankCard.getRank();
        }
        Plan lowerRankPlan = planQueryService.selectLowerDeliverLineRankPlan(projectId, referenceRank);
        if (null != lowerRankPlan) {
            if (null == lowerRank || lowerRank.compareTo(lowerRankPlan.getDeliverLineRank()) < 0) {
                lowerRank = lowerRankPlan.getDeliverLineRank();
            }
        }
        if (null == lowerRank) {
            lowerRank = START_RANK;
        }
        return lowerRank;
    }

    private Lock lockRank(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:project:rank:%s", projectId), 2, 2));
    }

    private Lock lockSeqNum(Long projectId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:project:seqNum:%s", projectId), 2, 2));
    }

    private Lock lockCardCommentSeqNum(Long cardId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:card:commentSeqNum:%s", cardId), 2, 2));
    }

    private Lock lockAncestorCommentSeqNum(Long ancestorId) {
        return lockFactory.getLock(new LockInfo(LockType.Reentrant,
                String.format("lock:card:ancestorCommentSeqNum:%s", ancestorId), 2, 2));
    }

    @Deprecated
    public static void setCardCreatedProps(Map<String, Object> cardDetail, String user, Card card) {
        cardDetail.put(CardField.COMPANY_ID, card.getCompanyId());
        cardDetail.put(CardField.PROJECT_ID, card.getProjectId());
        cardDetail.put(CardField.PLAN_ID, card.getPlanId());
        cardDetail.put(CardField.PARENT_ID, card.getParentId());
        cardDetail.put(CardField.STORY_MAP_NODE_ID, card.getStoryMapNodeId());
        cardDetail.put(CardField.SEQ_NUM, card.getSeqNum());
        cardDetail.put(CardField.CREATE_USER, user);
        cardDetail.put(CardField.CREATE_TIME, System.currentTimeMillis());
        cardDetail.put(CardField.LAST_MODIFY_USER, user);
        cardDetail.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        cardDetail.put(CardField.DELETED, false);
        cardDetail.remove(CardField.WATCH_USERS);
        cardDetail.remove(CardField.BPM_FLOW_ID);
        cardDetail.remove(CardField.BPM_FLOW_TO_STATUS);
    }

    public static void setCardCreatedProps(Map<String, Object> cardDetail, OperationContext opContext, Card card) {
        cardDetail.put(CardField.COMPANY_ID, card.getCompanyId());
        cardDetail.put(CardField.PROJECT_ID, card.getProjectId());
        cardDetail.put(CardField.PLAN_ID, card.getPlanId());
        cardDetail.put(CardField.PARENT_ID, card.getParentId());
        cardDetail.put(CardField.STORY_MAP_NODE_ID, card.getStoryMapNodeId());
        cardDetail.put(CardField.SEQ_NUM, card.getSeqNum());
        cardDetail.put(CardField.CREATE_USER, opContext.getUserName());
        cardDetail.put(CardField.CREATE_TIME, opContext.getCurrentTimeMillis());
        cardDetail.put(CardField.LAST_MODIFY_USER, opContext.getUserName());
        cardDetail.put(CardField.LAST_MODIFY_TIME, opContext.getCurrentTimeMillis());
        cardDetail.put(CardField.DELETED, false);
    }

    /**
     * without LAST_MODIFY_USER、LAST_MODIFY_TIME
     *
     * @param field
     * @param value
     * @return
     */
    public static Map<String, Object> generatePropsForUpdate(String field, Object value) {
        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(field, value);
        return cardProps;
    }

    @Deprecated
    public static Map<String, Object> generatePropsForUpdate(String user, String field, Object value) {
        Map<String, Object> cardProps = new HashMap<>();
        cardProps.put(CardField.LAST_MODIFY_USER, user);
        cardProps.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        cardProps.put(field, value);
        return cardProps;
    }

    @Deprecated
    public static Map<String, Object> generatePropsForUpdate(Map<String, Object> cardDetail, String user, String field, Object value) {
        cardDetail.put(CardField.LAST_MODIFY_USER, user);
        cardDetail.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        cardDetail.put(field, value);
        return cardDetail;
    }

    @Deprecated
    public static Map<String, Object> generatePropsForUpdate(Map<String, Object> cardDetail, String user, List<FieldChange> fieldChanges) {
        cardDetail.put(CardField.LAST_MODIFY_USER, user);
        cardDetail.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        fieldChanges.forEach(fieldChange -> cardDetail.put(fieldChange.getField().getKey(), fieldChange.getToValue()));
        return cardDetail;
    }

    public static Map<String, Object> generatePropsForUpdate(Map<String, Object> cardDetail, OperationContext opContext, List<FieldChange> fieldChanges) {
        cardDetail.put(CardField.LAST_MODIFY_USER, opContext.getUserName());
        cardDetail.put(CardField.LAST_MODIFY_TIME, opContext.getCurrentTimeMillis());
        fieldChanges.forEach(fieldChange -> cardDetail.put(fieldChange.getField().getKey(), fieldChange.getToValue()));
        return cardDetail;
    }

    public static Map<String, Object> generatePropsForUpdate(Map<String, Object> cardDetail, OperationContext opContext, String field, Object value) {
        cardDetail.put(CardField.LAST_MODIFY_USER, opContext.getUserName());
        cardDetail.put(CardField.LAST_MODIFY_TIME, opContext.getCurrentTimeMillis());
        cardDetail.put(field, value);
        return cardDetail;
    }

    @Deprecated
    public Map<String, Object> generatePropsForUpdateStatus(Map<String, Object> cardDetail, String user, String status, ProjectCardSchema schema) {
        cardDetail.put(CardField.LAST_MODIFY_USER, user);
        cardDetail.put(CardField.LAST_MODIFY_TIME, System.currentTimeMillis());
        cardDetail.put(CardField.STATUS, status);
        Map<String, Object> endStatusAndLastModifyTime = genEndStatus4OldCard(cardDetail, FieldUtil.toString(cardDetail.get(CardField.TYPE)), status, schema);
        cardDetail.putAll(endStatusAndLastModifyTime);
        return cardDetail;
    }

    public Map<String, Object> generatePropsForUpdateStatus(Map<String, Object> cardDetail, OperationContext opContext, String status, ProjectCardSchema schema) {
        cardDetail.put(CardField.LAST_MODIFY_USER, opContext.getUserName());
        cardDetail.put(CardField.LAST_MODIFY_TIME, opContext.getCurrentTimeMillis());
        cardDetail.put(CardField.STATUS, status);
        return cardDetail;
    }

    public static Set<String> getUsersFromCardFields(ProjectCardSchema schema, Map<String, Object> cardDetail) {
        Set<String> users = new HashSet<>();
        for (CardField field : schema.getFields()) {
            Object value = cardDetail.get(field.getKey());
            if (null == value) {
                continue;
            }
            switch (field.getType()) {
                case MEMBER:
                case USER:
                    users.add(FieldUtil.toString(value));
                    break;
                case MEMBERS:
                case USERS:
                    users.addAll(FieldUtil.toStringList(value));
            }
        }
        return users;
    }

    /**
     * 获取能获取普通消息（如：%s%s了%s卡片）的用户
     *
     * @param schema
     * @param cardDetail
     * @return
     */
    public static Set<String> getUsersForReceiveNormalMsg(ProjectCardSchema schema, Map<String, Object> cardDetail) {
        List<String> notNoticeFieldKeys = Arrays.asList("create_user", "at_users", "last_modify_user", "watch_users", "follow_users");
        Set<String> users = new HashSet<>();
        for (CardField field : schema.getFields()) {
            Object value = cardDetail.get(field.getKey());
            if (null == value) {
                continue;
            }
            if (notNoticeFieldKeys.contains(field.getKey())) {
                continue;
            }
            switch (field.getType()) {
                case MEMBER:
                case USER:
                    String user = FieldUtil.toString(value);
                    if (StringUtils.isNotEmpty(user)) {
                        users.add(user);
                    }
                    break;
                case MEMBERS:
                case USERS:
                    List<String> userList = FieldUtil.toStringList(value);
                    if (CollectionUtils.isNotEmpty(userList)) {
                        users.addAll(userList);
                    }
            }
        }
        return users;
    }

    public static String cardKey(Card card) {
        return String.format("%s-%s", card.getProjectKey(), card.getSeqNum());
    }

    public static List<Long> addRelateCardId(Map<String, Object> cardProps, Long relateCardId) {
        List<Long> relateCardIds = new ArrayList<>();
        relateCardIds.add(relateCardId);
        List<Long> fromRelateCardIds = FieldUtil.toLongList(cardProps.get(CardField.RELATED_CARD_IDS));
        if (CollectionUtils.isNotEmpty(fromRelateCardIds)) {
            relateCardIds.addAll(fromRelateCardIds);
        }
        cardProps.put(CardField.RELATED_CARD_IDS, relateCardIds);
        return relateCardIds.stream().distinct().collect(Collectors.toList());
    }

    public static List<Long> rmRelateCardId(Map<String, Object> cardProps, Long relateCardId) {
        return rmRelateCardIds(cardProps, Arrays.asList(relateCardId));
    }

    public static List<Long> rmRelateCardIds(Map<String, Object> cardProps, List<Long> relateCardIds) {
        List<Long> fromRelateCardIds = FieldUtil.toLongList(cardProps.get(CardField.RELATED_CARD_IDS));
        if (CollectionUtils.isEmpty(fromRelateCardIds)) {
            return ListUtils.EMPTY_LIST;
        }
        if (CollectionUtils.isNotEmpty(relateCardIds)) {
            fromRelateCardIds.removeAll(relateCardIds);
        }
        return fromRelateCardIds;
    }

    public static boolean isChangeToEnd(ProjectCardSchema schema, String type, String fromStatus, String toStatus) {
        List<String> endStatuses = schema.findCardType(type).getStatuses().stream().filter(s -> s.isEnd()).map(s -> s.getKey()).collect(Collectors.toList());
        return endStatuses.contains(toStatus) && !endStatuses.contains(fromStatus);
    }

    public static boolean isEnd(Map<String, Object> cardDetail) {
        return FieldUtil.toBoolean(cardDetail.get(CardField.CALC_IS_END));
    }


    protected static final String A_HREF_S_S_S_S_A = "<a href='%s'>[%s-%s] %s</a>";

    /**
     * 获取卡片的内部路径的html（超链接）
     *
     * @param card
     * @param endpointHelper
     * @param title
     * @return
     */
    public static String cardPathHtml(Card card, EndpointHelper endpointHelper, String title) {
        String url = endpointHelper.cardDetailPath(card.getProjectKey(), card.getSeqNum());
        return String.format(A_HREF_S_S_S_S_A, url, card.getProjectKey(), card.getSeqNum(), title);
    }

    /**
     * 获取卡片的内部链接的html（超链接）
     *
     * @param project
     * @param cardDetail
     * @param endpointHelper
     * @return
     */
    public static String cardPathHtml(Project project, Map<String, Object> cardDetail, EndpointHelper endpointHelper) {
        Long seqNum = FieldUtil.getSeqNum(cardDetail);
        String url = endpointHelper.cardDetailPath(project.getKey(), seqNum);
        String title = FieldUtil.getTitle(cardDetail);
        return String.format(A_HREF_S_S_S_S_A, url, project.getKey(), seqNum, title);
    }

    /**
     * 获取卡片的外部路径的html（超链接）
     *
     * @param card
     * @param endpointHelper
     * @param title
     * @return
     */
    public static String cardUrlHtml(Card card, EndpointHelper endpointHelper, String title) {
        String url = endpointHelper.cardDetailUrl(card.getCompanyId(), card.getProjectKey(), card.getSeqNum());
        return String.format(A_HREF_S_S_S_S_A, url, card.getProjectKey(), card.getSeqNum(), title);
    }

    /**
     * 获取卡片的外部路径的html（超链接）
     *
     * @param project
     * @param cardDetail
     * @param endpointHelper
     * @return
     */
    public static String cardUrlHtml(Project project, Map<String, Object> cardDetail, EndpointHelper endpointHelper) {
        Long seqNum = FieldUtil.getSeqNum(cardDetail);
        String url = endpointHelper.cardDetailUrl(project.getCompanyId(), project.getKey(), seqNum);
        String title = FieldUtil.getTitle(cardDetail);
        return String.format(A_HREF_S_S_S_S_A, url, project.getKey(), seqNum, title);
    }

    /**
     * 获取用户的外部路径的html（超链接）
     *
     * @param companyId
     * @param nickName
     * @param endpointHelper
     * @return
     */
    public static String userUrlHtml(Long companyId, String nickName, EndpointHelper endpointHelper) {
        return String.format("<a href='%s'>%s</a>",
                endpointHelper.userHomeUrl(companyId, nickName),
                nickName);
    }

}
