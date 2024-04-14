package com.ezone.ezproject.modules.card.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.AbstractCellWriteHandler;
import com.alibaba.excel.write.merge.AbstractMergeStrategy;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.ezone.devops.ezcode.base.util.Md5Util;
import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.storage.IStorage;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.CardExample;
import com.ezone.ezproject.dal.entity.CardRelateRel;
import com.ezone.ezproject.dal.entity.CardToken;
import com.ezone.ezproject.dal.entity.CardTokenExample;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.dal.mapper.CardTokenMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectWorkloadSetting;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.CardBeanWithRef;
import com.ezone.ezproject.modules.card.bean.CardKey;
import com.ezone.ezproject.modules.card.bean.ExportRequest;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.event.service.CardEventQueryService;
import com.ezone.ezproject.modules.card.excel.ExcelCardExport;
import com.ezone.ezproject.modules.card.excel.ExcelCardImport;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.card.field.limit.SysFieldOpLimit;
import com.ezone.ezproject.modules.common.EndpointHelper;
import com.ezone.ezproject.modules.company.service.CompanyProjectSchemaQueryService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.storymap.service.StoryMapQueryService;
import com.github.pagehelper.PageHelper;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardQueryService {
    private CardMapper cardMapper;

    private CardDao cardDao;

    private CardTokenMapper cardTokenMapper;

    private PlanQueryService planQueryService;

    private CardEventQueryService cardEventQueryService;

    private IStorage storage;

    private EndpointHelper endpointHelper;

    private ProjectQueryService projectQueryService;

    private CompanyProjectSchemaQueryService companyProjectSchemaQueryService;

    private ProjectMemberQueryService projectMemberQueryService;

    private CardRelateRelQueryService cardRelateRelQueryService;

    private StoryMapQueryService storyMapQueryService;

    private CompanyService companyService;

    private UserService userService;

    public Map<String, Object> selectDetail(Long id) {
        return cardDao.findAsMap(id);
    }

    public Map<String, Object> selectDetail(Long id, String... fields) throws IOException {
        return cardDao.findAsMap(id, fields);
    }

    @NotNull
    public CardToken selectEnsuredCardToken(Card card) {
        CardTokenExample example = new CardTokenExample();
        example.createCriteria().andCardIdEqualTo(card.getId());
        List<CardToken> cardTokens = cardTokenMapper.selectByExample(example);
        if (CollectionUtils.isNotEmpty(cardTokens)) {
            return cardTokens.get(0);
        }
        CardToken cardToken = CardToken.builder()
                .id(IdUtil.generateId())
                .cardId(card.getId())
                .token(Md5Util.getMD5Code(UUID.randomUUID().toString()))
                .projectId(card.getProjectId())
                .build();
        cardTokenMapper.insert(cardToken);
        return cardToken;
    }

    public List<CardToken> selectCardToken(List<Long> cardIds) {
        CardTokenExample example = new CardTokenExample();
        example.createCriteria().andCardIdIn(cardIds);
        return cardTokenMapper.selectByExample(example);
    }

    public CardBeanWithRef selectCardBeanWithRef(Card card) throws IOException {
        CardBeanWithRef.CardBeanWithRefBuilder builder = CardBeanWithRef.builder()
                .id(card.getId())
                .rank(card.getRank())
                .card(cardDao.findAsMap(card.getId()));
        if (card.getPlanId() > 0) {
            builder.plan(planQueryService.select(card.getPlanId()));
        }
        if (card.getParentId() > 0) {
            builder.parent(CardBean.builder().id(card.getParentId()).card(cardDao.findAsMap(card.getId(), CardField.TITLE)).build());
        }
        if (card.getStoryMapNodeId() > 0) {
            StoryMapNode storyMapL2Node = storyMapQueryService.selectStoryMapNodeById(card.getStoryMapNodeId());
            if (storyMapL2Node != null) {
                builder
                        .storyMap(storyMapQueryService.selectStoryMapById(storyMapL2Node.getStoryMapId()))
                        .storyMapL1Node(storyMapQueryService.selectStoryMapNodeById(storyMapL2Node.getParentId()))
                        .storyMapL2Node(storyMapL2Node);
            }
        }
        return builder.build();
    }

    public Card select(Long id) {
        if (null == id || id <= 0) {
            return null;
        }
        return cardMapper.selectByPrimaryKey(id);
    }

    public List<Card> select(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ListUtils.EMPTY_LIST;
        }
        CardExample example = new CardExample();
        example.createCriteria().andIdIn(ids);
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectAll(Integer pageNumber, Integer pageSize) {
        CardExample example = new CardExample();
        PageHelper.startPage(pageNumber, pageSize, true);
        return cardMapper.selectByExample(example);
    }

    @NotNull
    public Map<Long, Map<String, Object>> selectDetail(List<Long> ids, String... fields) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return MapUtils.EMPTY_MAP;
        }
        return cardDao.findAsMap(ids, fields);
    }

    @NotNull
    public Map<Long, String> selectTitle(List<Long> ids) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return MapUtils.EMPTY_MAP;
        }
        return cardDao.findAsMap(ids, CardField.TITLE)
                .entrySet().stream().collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> FieldUtil.getTitle(e.getValue())));
    }

    public String selectTitle(Long id) throws IOException {
        return FieldUtil.getTitle(cardDao.findAsMap(id, CardField.TITLE));
    }

    public Card select(Long company, String projectKey, Long seqNum) {
        CardExample example = new CardExample();
        example.createCriteria().andCompanyIdEqualTo(company).andProjectKeyEqualTo(projectKey).andSeqNumEqualTo(seqNum);
        List<Card> cards = cardMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cards)) {
            return null;
        }
        return cards.get(0);
    }

    public List<Card> select(Long company, List<String> cardStringKeys) {
        if (CollectionUtils.isEmpty(cardStringKeys)) {
            return ListUtils.EMPTY_LIST;
        }
        CardExample example = new CardExample();
        boolean valid = false;
        for (String cardStringKey : cardStringKeys) {
            String projectKey = StringUtils.substringBeforeLast(cardStringKey, "-");
            Long seqNum = NumberUtils.toLong(StringUtils.substringAfterLast(cardStringKey, "-"));
            if (seqNum > 0) {
                valid = true;
                example.or()
                        .andCompanyIdEqualTo(company)
                        .andProjectKeyEqualTo(projectKey).andSeqNumEqualTo(seqNum);
            }
        }
        if (!valid) {
            return ListUtils.EMPTY_LIST;
        }
        return cardMapper.selectByExample(example);
    }

    public List<Long> selectIdsByKeys(Long company, List<String> cardStringKeys) {
        return select(company, cardStringKeys).stream().map(Card::getId).collect(Collectors.toList());
    }

    /**
     * 校验规则：卡片存在&在公司下&用户是卡片所属项目的成员
     *
     * @param company
     * @param user
     * @param cardStringKeys
     * @return
     */
    public List<String> filter(Long company, String user, List<String> cardStringKeys) {
        if (CollectionUtils.isEmpty(cardStringKeys)) {
            return ListUtils.EMPTY_LIST;
        }
        List<CardKey> cardKeys = new ArrayList<>();
        List<String> projectKeys = new ArrayList<>();

        for (String cardStringKey : cardStringKeys) {
            String projectKey = StringUtils.substringBeforeLast(cardStringKey, "-");
            Long seqNum = NumberUtils.toLong(StringUtils.substringAfterLast(cardStringKey, "-"));
            if (seqNum > 0) {
                projectKeys.add(projectKey);
                cardKeys.add(CardKey.builder().projectKey(projectKey).seqNum(seqNum).build());
            }
        }
        if (CollectionUtils.isEmpty(cardKeys)) {
            return ListUtils.EMPTY_LIST;
        }

        List<Project> projects = projectQueryService.select(company, projectKeys);
        if (CollectionUtils.isEmpty(projects)) {
            return ListUtils.EMPTY_LIST;
        }

        List<String> validProjectKeys;
        if (!userService.isCompanyAdmin(user, company)) {
            List<Long> userProjectIds = projectMemberQueryService.selectUserRoleProjectIds(company, user);
            if (CollectionUtils.isEmpty(userProjectIds)) {
                return ListUtils.EMPTY_LIST;
            }
            validProjectKeys = projects.stream()
                    .filter(p -> userProjectIds.contains(p.getId()))
                    .map(Project::getKey)
                    .collect(Collectors.toList());
        } else {
            validProjectKeys = projects.stream()
                    .map(Project::getKey)
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isEmpty(validProjectKeys)) {
            return ListUtils.EMPTY_LIST;
        }
        CardExample example = new CardExample();
        cardKeys.stream().filter(k -> validProjectKeys.contains(k.getProjectKey()))
                .forEach(k -> example
                        .or()
                        .andCompanyIdEqualTo(company)
                        .andProjectKeyEqualTo(k.getProjectKey())
                        .andSeqNumEqualTo(k.getSeqNum()));
        List<Card> cards = cardMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cards)) {
            return ListUtils.EMPTY_LIST;
        }
        return cards.stream()
                .map(card -> String.format("%s-%s", card.getProjectKey(), card.getSeqNum()))
                .collect(Collectors.toList());
    }

    public void check(Long projectId, List<Long> cardIds) {
        if (CollectionUtils.isEmpty(cardIds)) {
            return;
        }
        List<Card> cards = select(cardIds);
        if (CollectionUtils.isEmpty(cards) || cards.size() < cardIds.size()) {
            throw CodedException.NOT_FOUND;
        }
        if (cards.stream().anyMatch(card -> !card.getProjectId().equals(projectId))) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "卡片不在项目下！");
        }
    }

    public List<Card> selectByProjectId(Long projectId) {
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andDeletedEqualTo(false);
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectByPlanId(Long planId) {
        return selectByPlanId(planId, false);
    }

    public List<Card> selectByPlanId(Long planId, Boolean isDelete) {
        CardExample example = new CardExample();
        if (isDelete != null) {
            example.createCriteria().andPlanIdEqualTo(planId).andDeletedEqualTo(isDelete);
        }
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectNoPlan(Long projectId) {
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andPlanIdEqualTo(0L).andDeletedEqualTo(false);
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectByPlanIds(List<Long> planIds) {
        if (CollectionUtils.isEmpty(planIds)) {
            return Collections.emptyList();
        }
        CardExample example = new CardExample();
        example.createCriteria().andPlanIdIn(planIds).andDeletedEqualTo(false);
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectAllByPlanIds(List<Long> planIds) {
        if (CollectionUtils.isEmpty(planIds)) {
            return Collections.emptyList();
        }
        CardExample example = new CardExample();
        example.createCriteria().andPlanIdIn(planIds);
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectByPlanIds(Long projectId, List<Long> planIds) {
        if (CollectionUtils.isEmpty(planIds)) {
            return Collections.emptyList();
        }
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andPlanIdIn(planIds).andDeletedEqualTo(false);
        return cardMapper.selectByExample(example);
    }

    /**
     * 所有子孙计划(包括已归档)下的未删除的卡片
     *
     * @param ancestorPlan
     * @return
     */
    public long countByAncestorPlan(Plan ancestorPlan) {
        List<Long> planIds = new ArrayList<>();
        planIds.add(ancestorPlan.getId());
        List<Plan> plans = planQueryService.selectDescendant(ancestorPlan, null);
        if (CollectionUtils.isNotEmpty(plans)) {
            plans.forEach(p -> planIds.add(p.getId()));
        }
        CardExample example = new CardExample();
        example.createCriteria().andPlanIdIn(planIds).andDeletedEqualTo(false);
        return cardMapper.countByExample(example);
    }

    public Card selectHighestRankCard(Long projectId, Long planId) {
        CardExample example = new CardExample();
        if (planId != null && planId > 0) {
            example.createCriteria().andPlanIdEqualTo(planId);
        } else {
            example.createCriteria().andProjectIdEqualTo(projectId).andPlanIdEqualTo(0L);
        }
        example.setOrderByClause("`rank` desc");
        PageHelper.startPage(1, 1, false);
        List<Card> cards = cardMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cards)) {
            return null;
        }
        return cards.get(0);
    }

    public Card selectLowestRankCard(Long projectId, Long planId) {
        CardExample example = new CardExample();
        if (planId != null && planId > 0) {
            example.createCriteria().andPlanIdEqualTo(planId);
        } else {
            example.createCriteria().andProjectIdEqualTo(projectId).andPlanIdEqualTo(0L);
        }
        example.setOrderByClause("`rank` asc");
        PageHelper.startPage(1, 1, false);
        List<Card> cards = cardMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cards)) {
            return null;
        }
        return cards.get(0);
    }

    public Card selectHigherRankCard(Long projectId, String rank) {
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRankGreaterThan(rank);
        example.setOrderByClause("`rank` asc");
        PageHelper.startPage(1, 1, false);
        List<Card> cards = cardMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cards)) {
            return null;
        }
        return cards.get(0);
    }

    public List<Card> selectHigherOrEqualRankCardLimit2(Long projectId, String rank) {
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRankGreaterThanOrEqualTo(rank);
        example.setOrderByClause("`rank` asc");
        PageHelper.startPage(1, 2, false);
        return cardMapper.selectByExample(example);
    }

    public Card selectLowerRankCard(Long projectId, String rank) {
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRankLessThan(rank);
        example.setOrderByClause("`rank` desc");
        PageHelper.startPage(1, 1, false);
        List<Card> cards = cardMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(cards)) {
            return null;
        }
        return cards.get(0);
    }

    public List<Card> selectLowerOrEqualRankCardLimit2(Long projectId, String rank) {
        CardExample example = new CardExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andRankLessThanOrEqualTo(rank);
        example.setOrderByClause("`rank` asc");
        PageHelper.startPage(1, 2, false);
        return cardMapper.selectByExample(example);
    }

    /**
     * 子卡片
     */
    public List<Card> selectChildren(Long parentId) {
        CardExample example = new CardExample();
        example.createCriteria().andParentIdEqualTo(parentId);
        return cardMapper.selectByExample(example);
    }

    /**
     * 子孙卡片
     *
     * @param ancestorId 一级祖先卡片
     * @return
     */
    public List<Card> selectDescendant(Long ancestorId) {
        CardExample example = new CardExample();
        example.createCriteria().andAncestorIdEqualTo(ancestorId);
        return cardMapper.selectByExample(example);
    }

    /**
     * 子孙卡片
     *
     * @param ancestorId 一级祖先卡片
     * @return
     */
    public List<Card> selectDescendantByIds(List<Long> ancestorId) {
        CardExample example = new CardExample();
        example.createCriteria().andAncestorIdIn(ancestorId);
        return cardMapper.selectByExample(example);
    }

    /**
     * 子孙卡片
     *
     * @param ancestorId 一级祖先卡片
     * @return
     */
    private List<Card> selectDescendantAndOwner(List<Long> ancestorId) {
        CardExample example = new CardExample();
        example.or().andAncestorIdIn(ancestorId);
        example.or().andIdIn(ancestorId);
        return cardMapper.selectByExample(example);
    }

    /**
     * 子孙卡片
     *
     * @param card
     * @return
     */
    public List<Card> selectDescendant(Card card) {
        if (card.getAncestorId().equals(0L)) {
            return selectDescendant(card.getId());
        }
        Map<Long, List<Card>> tree = selectDescendant(card.getAncestorId()).stream().collect(Collectors.groupingBy(Card::getParentId));
        List<Card> descendant = new ArrayList<>();
        collectDescendant(tree.get(card.getId()), tree, descendant);
        return descendant;
    }

    /**
     * 子孙卡片
     *
     * @param cards
     * @return map key-原卡片对应ID，id对应卡片的子孙卡片
     */
    public Map<Long, List<Card>> selectDescendant(List<Card> cards) {
        ArrayList<Long> ancestorIds = cards.stream()
                .map(card -> card.getAncestorId().equals(0L) ? card.getId() : card.getAncestorId())
                .collect(Collectors.collectingAndThen(Collectors.toSet(), ArrayList::new));

        List<Card> sameAncestorCards = selectDescendantByIds(ancestorIds);
        Map<Long, List<Card>> tree = sameAncestorCards.stream().collect(Collectors.groupingBy(Card::getParentId));

        Map<Long, List<Card>> descendantMap = new HashMap<>();
        for (Card card : cards) {
            List<Card> descendant = new ArrayList<>();
            collectDescendant(tree.get(card.getId()), tree, descendant);
            descendantMap.put(card.getId(), descendant);
        }

        return descendantMap;
    }

    /**
     * 查询卡片所有的祖先卡片
     *
     * @param cards
     * @return map key-原卡片对应ID，id对应卡片的祖先卡片
     */
    public Map<Long, List<Card>> selectAncestor(List<Card> cards) {
        ArrayList<Long> ancestorIds = cards.stream()
                .map(card -> card.getAncestorId().equals(0L) ? card.getId() : card.getAncestorId())
                .collect(Collectors.collectingAndThen(Collectors.toSet(), ArrayList::new));
        List<Card> sameTreeCards = selectDescendantAndOwner(ancestorIds);
        Map<Long, Card> idCardMap = sameTreeCards.stream().collect(Collectors.toMap(Card::getId, card -> card));

        Map<Long, List<Card>> ancestorMap = new HashMap<>();
        for (Card card : cards) {
            List<Card> ancestor = new ArrayList<>();
            collectAncestor(idCardMap.get(card.getId()), idCardMap, ancestor);
            ancestorMap.put(card.getId(), ancestor);
        }
        return ancestorMap;
    }

    private void collectDescendant(List<Card> children, Map<Long, List<Card>> tree, List<Card> descendant) {
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        descendant.addAll(children);
        children.forEach(child -> collectDescendant(tree.get(child.getId()), tree, descendant));
    }

    private void collectAncestor(Card card, Map<Long, Card> idCardMap, List<Card> ancestor) {
        if (card == null || card.getParentId().equals(0L)) {
            return;
        }
        Card parentCard = idCardMap.get(card.getParentId());
        ancestor.add(parentCard);
        collectAncestor(parentCard, idCardMap, ancestor);
    }


    public List<Card> selectByAncestorIds(List<Long> ancestorIds) {
        CardExample example = new CardExample();
        example.createCriteria().andAncestorIdIn(ancestorIds);
        return cardMapper.selectByExample(example);
    }

    public Map<Long, List<Card>> selectDescendants(List<Long> ancestorIds) {
        return selectByAncestorIds(ancestorIds).stream().collect(Collectors.groupingBy(Card::getAncestorId));
    }


    public void writeImportExcelTemplate(ProjectCardSchema schema, ProjectWorkloadSetting workloadSetting, List<String> cardTypes, OutputStream excel) {
        List<String> comment;
        List<String> enabledFieldNames = new ArrayList<>();
        boolean isEnableIncrWorkload = workloadSetting != null && workloadSetting.isEnableIncrWorkload();
        final List<String> requiredFieldNames = new ArrayList<>();
        final List<String> requiredFieldKeys = new ArrayList<>();
        if (cardTypes.isEmpty()) {
            throw CodedException.BAD_REQUEST;
        } else if (cardTypes.size() == 1) {
            comment = Collections.singletonList("说明（导入时请删除此行）：\n" +
                    "1. 标记红色字段为必填项；黄色为选填项但不可以删除。\n" +
                    "2. 如有日期类型(如计划开始/结束时间)，格式为" + ExcelCardImport.IMPORT_DATE_FORMAT + "形式，如" + ExcelCardImport.IMPORT_DATE_FORMAT_EXAMPLE + "。\n" +
                    "3. 如有时间类型，格式为" + ExcelCardImport.IMPORT_DATE_TIME_FORMAT + "形式，如" + ExcelCardImport.IMPORT_DATE_TIME_FORMAT_EXAMPLE + "。\n" +
                    "4. 人员类型字段匹配规则为用户名，如有多个需要用\"英文逗号\"分隔，如zhangsan,lisi,wangwu。\n" +
                    "5. 若该工作项为子卡片，则“父卡片编号”列需填写第一列中的编号，并将所有子工作项紧跟其父卡片。");
            String cardType = cardTypes.get(0);
            CardType cardTypeObj = schema.findCardType(cardType);
            if (cardTypeObj == null) {
                throw CodedException.ERROR_CARD_TYPE;
            }
            List<CardType.FieldConf> fields = cardTypeObj.getFields();
            requiredFieldKeys.addAll(cardTypeObj.findRequiredFields("open"));
            requiredFieldKeys.add(CardField.STATUS);
            enabledFieldNames.add(ExcelCardImport.TITLE_CARD_TEMP_NUM_INDEX, ExcelCardImport.TITLE_FIELD_CARD_TEMP_NUM);
            requiredFieldNames.add(ExcelCardImport.TITLE_FIELD_CARD_TEMP_NUM);
            enabledFieldNames.add(ExcelCardImport.TITLE_PARENT_CARD_TEMP_NUM_INDEX, ExcelCardImport.TITLE_FIELD_PARENT_CASE_NUM);
            requiredFieldKeys.stream()
                    .map(k -> schema.findCardField(k).getName())
                    .forEach(fieldName -> {
                        enabledFieldNames.add(fieldName);
                        requiredFieldNames.add(fieldName);
                    });
            fields.stream()
                    .filter(CardType.FieldConf::isEnable)
                    .filter(f -> SysFieldOpLimit.canOp(f.getKey(), SysFieldOpLimit.Op.IMPORT))
                    .filter(f -> !CardField.TYPE.equals(f.getKey()))
                    .filter(f -> !requiredFieldKeys.contains(f.getKey()))
                    .filter(f -> !isEnableIncrWorkload || !CardField.ACTUAL_WORKLOAD.equals(f.getKey()))
                    .forEach(f -> enabledFieldNames.add(schema.findCardField(f.getKey()).getName()));
        } else {
            comment = Collections.singletonList("说明（导入时请删除此行）：\n" +
                    "1. 卡片类型、标题、流程状态为所有卡片类型的必填项，卡片类型不同时不确定其他字段是否必填，故不在提示，但是在导入时仍然根据卡片类型校验必填项，若必填项未填写将会导致这行数据导入失败\n" +
                    "2. 如有日期类型(如计划开始/结束时间)，格式为" + ExcelCardImport.IMPORT_DATE_FORMAT + "形式，如" + ExcelCardImport.IMPORT_DATE_FORMAT_EXAMPLE + "。\n" +
                    "3. 如有时间类型，格式为" + ExcelCardImport.IMPORT_DATE_TIME_FORMAT + "形式，如" + ExcelCardImport.IMPORT_DATE_TIME_FORMAT_EXAMPLE + "。\n" +
                    "4. 人员类型字段匹配规则为用户名，如有多个需要用\"英文逗号\"分隔，如zhangsan,lisi,wangwu。\n" +
                    "5. 若该工作项为子卡片，则“父卡片编号”列需填写第一列中的编号，并将所有子工作项紧跟其父卡片。");
            requiredFieldKeys.addAll(Arrays.asList(CardField.TITLE, CardField.TYPE, CardField.STATUS));
            requiredFieldNames.add(ExcelCardImport.TITLE_FIELD_CARD_TEMP_NUM);
            enabledFieldNames.add(ExcelCardImport.TITLE_CARD_TEMP_NUM_INDEX, ExcelCardImport.TITLE_FIELD_CARD_TEMP_NUM);
            enabledFieldNames.add(ExcelCardImport.TITLE_PARENT_CARD_TEMP_NUM_INDEX, ExcelCardImport.TITLE_FIELD_PARENT_CASE_NUM);
            requiredFieldKeys.stream()
                    .map(k -> schema.findCardField(k).getName())
                    .forEach(fieldName -> {
                        enabledFieldNames.add(fieldName);
                        requiredFieldNames.add(fieldName);
                    });
            cardTypes.forEach(cardType -> {
                CardType cardTypeObj = schema.findCardType(cardType);
                if (cardTypeObj == null) {
                    throw CodedException.ERROR_CARD_TYPE;
                }
                cardTypeObj.getFields().stream()
                        .filter(CardType.FieldConf::isEnable)
                        .filter(f -> SysFieldOpLimit.canOp(f.getKey(), SysFieldOpLimit.Op.IMPORT))
                        .filter(f -> !requiredFieldKeys.contains(f.getKey()))
                        .filter(f -> !enabledFieldNames.contains(schema.findCardField(f.getKey()).getName()))
                        .filter(f -> !isEnableIncrWorkload || !CardField.ACTUAL_WORKLOAD.equals(f.getKey()))
                        .forEach(f -> enabledFieldNames.add(schema.findCardField(f.getKey()).getName()));
            });
        }

        List<CellRangeAddress> cellRangeAddresses = new ArrayList<>();
        //对导入说明的位置进行单元格合并。
        cellRangeAddresses.add(new CellRangeAddress(1, 1, 0, enabledFieldNames.size() - 1));
        MyMergeStrategy myMergeStrategy = new MyMergeStrategy(cellRangeAddresses);

        EasyExcel.write(excel)
                .registerWriteHandler(myMergeStrategy)
                .registerWriteHandler(new CellStyleWriteHandler(requiredFieldNames))
                .sheet("sheet1")
                .doWrite(Arrays.asList(enabledFieldNames, comment));
    }


    public void writeExportExcel(Long projectId, ProjectCardSchema schema, ExportRequest request, OutputStream excel)
            throws IOException {
        List<Long> ids = request.getIds();
        List<Card> cards = select(ids);
        cards = cards.stream().sorted(Comparator.comparingInt(card -> ids.indexOf(card.getId()))).collect(Collectors.toList());
        cards.forEach(card -> {
            if (!projectId.equals(card.getProjectId())) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Card not in the project!");
            }
        });
        List<CardField> fields = schema.getFields().stream()
                .filter(f -> {
                    switch (f.getKey()) {
                        case CardField.CONTENT:
                            return request.isExportContent();
                        case CardField.DELETED:
                            return false;
                        default:
                            return request.isExportAllFields() || request.getExportFields().contains(f.getKey());
                    }
                })
                .collect(Collectors.toList());
        Project project = projectQueryService.select(projectId);
        String company = companyService.companyName(project.getCompanyId());
        ExcelCardExport.builder()
                .companyName(company)
                .companySchema(companyProjectSchemaQueryService.getCompanyCardSchema(project.getCompanyId()))
                .project(project)
                .schema(schema)
                .cards(cards)
                .fields(fields)
                .exportOperations(request.isExportOperations())
                .cardDao(cardDao)
                .findCardByIds(this::select)
                .findPlanByIds(planQueryService::select)
                .findStoryMapNodeByIds(storyMapQueryService::selectStoryMapNodeByIds)
                .findRelateCardKeys(this::findRelateCardKeys)
                .findEventByCardId(cardEventQueryService::selectByCardId)
                .generateCardUrl(card -> endpointHelper.cardDetailUrl(card.getCompanyId(), card.getProjectKey(), card.getSeqNum()))
                .build()
                .writeExportExcel(excel);
    }

    private Map<Long, List<String>> findRelateCardKeys(List<Long> cardIds) {
        List<CardRelateRel> rels = cardRelateRelQueryService.selectByCardId(cardIds);
        if (CollectionUtils.isEmpty(rels)) {
            return MapUtils.EMPTY_MAP;
        }
        List<Card> relateCards = select(rels.stream().map(r -> r.getRelatedCardId()).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(relateCards)) {
            return MapUtils.EMPTY_MAP;
        }
        Map<Long, String> relateCardKeys = relateCards.stream().collect(Collectors.toMap(Card::getId, CardHelper::cardKey));
        return rels.stream().collect(Collectors
                .groupingBy(r -> r.getCardId(), Collectors
                        .mapping(r -> relateCardKeys.get(r.getRelatedCardId()), Collectors.toList())));
    }

    private Map<Long, List<Card>> findRelateCards(List<Long> cardIds) {
        List<CardRelateRel> rels = cardRelateRelQueryService.selectByCardId(cardIds);
        if (CollectionUtils.isEmpty(rels)) {
            return MapUtils.EMPTY_MAP;
        }
        List<Card> relateCards = select(rels.stream().map(r -> r.getRelatedCardId()).collect(Collectors.toList()));
        if (CollectionUtils.isEmpty(relateCards)) {
            return MapUtils.EMPTY_MAP;
        }
        Map<Long, Card> relateCardMap = relateCards.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
        return rels.stream().collect(Collectors
                .groupingBy(r -> r.getCardId(), Collectors
                        .mapping(r -> relateCardMap.get(r.getRelatedCardId()), Collectors.toList())));
    }

    public ResponseEntity downloadImportFailExcel(String errorStoragePath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDispositionFormData("attachment", StringUtils.substringAfterLast(errorStoragePath, "/"));
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        InputStreamResource inputStreamResource = new InputStreamResource(storage.open(errorStoragePath));
        return new ResponseEntity<>(inputStreamResource, headers, HttpStatus.OK);
    }

    public List<Card> selectByStoryMapNodeId(List<Long> storyMapNodeIds) {
        CardExample example = new CardExample();
        example.createCriteria().andStoryMapNodeIdIn(storyMapNodeIds).andDeletedEqualTo(false);
        return cardMapper.selectByExample(example);
    }

    public List<Card> selectByStoryMapNodeId(Long storyMapNodeId) {
        CardExample example = new CardExample();
        example.createCriteria().andStoryMapNodeIdEqualTo(storyMapNodeId).andDeletedEqualTo(false);
        return cardMapper.selectByExample(example);
    }

    public long countByStoryMapNodeId(Long storyMapNodeId) {
        CardExample example = new CardExample();
        example.createCriteria().andStoryMapNodeIdEqualTo(storyMapNodeId).andDeletedEqualTo(false);
        return cardMapper.countByExample(example);
    }

    public long countByStoryMapNodeId(List<Long> storyMapNodeIds) {
        CardExample example = new CardExample();
        example.createCriteria().andStoryMapNodeIdIn(storyMapNodeIds).andDeletedEqualTo(false);
        return cardMapper.countByExample(example);
    }

    public long count(Query... queries) throws IOException {
        return cardDao.count(Arrays.asList(queries));
    }

    public Map<String, Long> countGroupBy(String groupByField, int groupSize, Query... queries) throws Exception {
        return cardDao.countGroupBy(groupByField, groupSize, queries);
    }

    public boolean isWatch(Long cardId) throws IOException {
        List<String> watchUsers = FieldUtil.getWatchUsers(cardDao.findAsMap(cardId, CardField.WATCH_USERS));
        return watchUsers != null && watchUsers.contains(userService.currentUserName());
    }

    /**
     * 合并单元格
     */
    static class MyMergeStrategy extends AbstractMergeStrategy {

        //合并坐标集合
        @Setter
        private List<CellRangeAddress> cellRangeAddress;

        //构造
        public MyMergeStrategy(List<CellRangeAddress> cellRangeAddress) {
            this.cellRangeAddress = cellRangeAddress;
        }

        @Override
        protected void merge(Sheet sheet, Cell cell, Head head, Integer integer) {
            if (CollectionUtils.isNotEmpty(cellRangeAddress)) {
                if (cell.getRowIndex() == 1) {
                    for (CellRangeAddress item : cellRangeAddress) {
                        sheet.addMergedRegionUnsafe(item);
                    }
                }
            }
        }
    }

    /**
     * 设置样式：表头及导入说明的样式
     */
    static class CellStyleWriteHandler extends AbstractCellWriteHandler {
        private List<String> requiredFieldNames;

        CellStyleWriteHandler(List<String> requiredFieldNames) {
            this.requiredFieldNames = requiredFieldNames;
        }

        @Override
        public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                     List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                     Integer relativeRowIndex, Boolean isHead) {
            Workbook workbook = writeSheetHolder.getSheet().getWorkbook();
            CellStyle defaultCellStyle = workbook.createCellStyle();
            defaultCellStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            writeSheetHolder.getSheet().setDefaultColumnStyle(cell.getColumnIndex(), defaultCellStyle);
            CellStyle cellStyle = workbook.createCellStyle();
            if (cell.getRowIndex() == 0) {
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                short color = requiredFieldNames.contains(cell.getStringCellValue()) ?
                        IndexedColors.RED.getIndex() : IndexedColors.YELLOW.getIndex();

                cellStyle.setFillForegroundColor(color);
                cell.setCellStyle(cellStyle);
                cell.setCellType(CellType.STRING);
            } else {
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cellStyle.setWrapText(true); // 设置折行显示
                cell.setCellStyle(cellStyle);
                cell.setCellType(CellType.STRING);
                cell.getRow().setHeightInPoints(100f);
            }
        }

    }
}
