package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.CardBean;
import com.ezone.ezproject.modules.card.bean.ReferenceValues;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.card.field.FieldUtil;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class CardSearchService {
    private CardDao cardDao;

    private UserService userService;

    private CompanyService companyService;

    private CardQueryService cardQueryService;

    private PlanQueryService planQueryService;

    private ProjectCardSchemaHelper projectCardSchemaHelper;

    private CardReferenceValueHelper referenceValueHelper;

    private CardRelateRelQueryService cardRelateRelQueryService;

    private ProjectMemberQueryService projectMemberQueryService;

    public TotalBean<CardBean> search(Plan plan, boolean containsDescendantPlan, SearchEsRequest searchCardRequest)
            throws IOException {
        List<Long> planIds = new ArrayList<>();
        planIds.add(plan.getId());
        if (containsDescendantPlan) {
            List<Plan> descendants = planQueryService.selectDescendant(plan.getId(), plan.getIsActive());
            if (CollectionUtils.isNotEmpty(descendants)) {
                planIds.addAll(descendants.stream().map(Plan::getId).collect(Collectors.toList()));
            }
        }
        return searchCards(cardQueryService.selectByPlanIds(planIds), searchCardRequest);
    }

    public TotalBean<CardBean> searchNoPlan(Long projectId, SearchEsRequest searchCardRequest)
            throws IOException {
        return searchCards(cardQueryService.selectNoPlan(projectId), searchCardRequest);
    }

    public TotalBean<CardBean> searchByActiveAndNoPlan(Long projectId, SearchEsRequest searchCardRequest)
            throws IOException {
        List<Long> planIds = new ArrayList<>();
        planIds.add(0L);
        planQueryService.selectByProjectId(projectId, true).forEach(plan -> planIds.add(plan.getId()));
        return searchCards(cardQueryService.selectByPlanIds(projectId, planIds), searchCardRequest);
    }

    public @NotNull TotalBean<CardBean> search(Long projectId, SearchEsRequest searchCardRequest, Boolean deleted,
                                      Integer pageNumber, Integer pageSize) throws IOException {
        return search(Arrays.asList(projectId), searchCardRequest, deleted, pageNumber, pageSize);
    }

    public TotalBean<CardBean> search(SearchEsRequest searchCardRequest,
                                      Integer pageNumber, Integer pageSize) throws IOException {
        TotalBean<CardBean> totalBean = cardDao.search(searchCardRequest, pageNumber, pageSize);
        referenceValues(totalBean, searchCardRequest.getFields());
        return totalBean;
    }

    public TotalBean<CardBean> search(List<Long> projectIds, SearchEsRequest searchCardRequest, Boolean deleted,
                                      Integer pageNumber, Integer pageSize) throws IOException {
        if (CollectionUtils.isEmpty(projectIds)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<Query> queries = new ArrayList<>();
        queries.add(In.builder().field(CardField.PROJECT_ID).values(projectIds.stream().map(String::valueOf).collect(Collectors.toList())).build());
        if (CollectionUtils.isNotEmpty(searchCardRequest.getQueries())) {
            queries.addAll(searchCardRequest.getQueries());
        }
        if (deleted != null) {
            queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(deleted)).build());
        }
        searchCardRequest.setQueries(queries);
        TotalBean<CardBean> totalBean = cardDao.search(searchCardRequest, pageNumber, pageSize);
        referenceValues(totalBean, searchCardRequest.getFields());
        return totalBean;
    }

    public TotalBean<CardBean> search(List<Long> cardIds, String... fields) throws IOException {
        TotalBean<CardBean> totalBean = cardDao.search(cardIds, SearchEsRequest.builder().fields(fields).build(), 1, cardIds.size());
        referenceValues(totalBean, fields);
        return totalBean;
    }


    public TotalBean<CardBean> searchByCompany(Long companyId, SearchEsRequest searchCardRequest, Boolean deleted,
                                               Integer pageNumber, Integer pageSize) throws IOException {
        List<Query> queries = new ArrayList<>();
        queries.add(Eq.builder().field(CardField.COMPANY_ID).value(String.valueOf(companyId)).build());
        if (CollectionUtils.isNotEmpty(searchCardRequest.getQueries())) {
            queries.addAll(searchCardRequest.getQueries());
        }
        if (deleted != null) {
            queries.add(Eq.builder().field(CardField.DELETED).value(String.valueOf(deleted)).build());
        }
        searchCardRequest.setQueries(queries);
        TotalBean<CardBean> totalBean = cardDao.search(searchCardRequest, pageNumber, pageSize);
        referenceValues(totalBean, searchCardRequest.getFields());
        return totalBean;
    }

    /**
     * 查询用户有权限的项目中的卡片
     *
     * @param searchCardRequest
     * @param deleted
     * @param pageNumber
     * @param pageSize
     * @return
     * @throws IOException
     */
    public TotalBean<CardBean> searchUserProjectCards(SearchEsRequest searchCardRequest, Boolean deleted,
                                                      Integer pageNumber, Integer pageSize) throws IOException {
        List<Long> userProjectIds;
        Long company = companyService.currentCompany();
        String user = userService.currentUserName();
        if (userService.isCompanyAdmin(user, company)) {
            return searchByCompany(company, searchCardRequest, deleted, pageNumber, pageSize);
        } else {
            userProjectIds = projectMemberQueryService.selectUserRoleProjectIds(company, user);
        }
        if (CollectionUtils.isEmpty(userProjectIds)) {
            return TotalBean.<CardBean>builder().build();
        }
        return search(userProjectIds, searchCardRequest, deleted, pageNumber, pageSize);
    }

    public TotalBean<CardBean> search(Long projectId, List<Long> cardIds, String[] fields) throws IOException {
        if (CollectionUtils.isEmpty(cardIds)) {
            return TotalBean.<CardBean>builder().build();
        }
        cardQueryService.select(cardIds).forEach(card -> {
            if (!card.getProjectId().equals(projectId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Card not in project!");
            }
        });
        Map<Long, Map<String, Object>> cards = cardDao.findAsMap(cardIds, fields);
        if (MapUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<CardBean> cardBeans = cards.entrySet().stream()
                .map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())
                .collect(Collectors.toList());
        TotalBean<CardBean> totalBean = TotalBean.<CardBean>builder().total(cardBeans.size()).list(cardBeans).build();
        referenceValues(totalBean, fields);
        return totalBean;
    }

    public TotalBean<CardBean> searchRelateCards(Long cardId, String[] fields) throws IOException {
        List<Long> relatedCards = cardRelateRelQueryService.selectRelateCardIds(cardId);
        if (CollectionUtils.isEmpty(relatedCards)) {
            return TotalBean.<CardBean>builder().build();
        }
        Map<Long, Map<String, Object>> cards = cardDao.findAsMap(relatedCards, fields);
        if (MapUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<CardBean> cardBeans = cards.entrySet().stream()
                .map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())
                .collect(Collectors.toList());
        TotalBean<CardBean> totalBean = TotalBean.<CardBean>builder().total(cardBeans.size()).list(cardBeans).build();
        referenceValues(totalBean, fields);
        return totalBean;
    }

    public TotalBean<CardBean> searchTreeCards(Card card, String[] fields) throws IOException {
        Long ancestorId = card.getParentId() > 0 ? card.getAncestorId() : card.getId();
        List<Long> ids = new ArrayList<>();
        ids.add(ancestorId);
        List<Card> descendant = cardQueryService.selectDescendant(ancestorId);
        if (CollectionUtils.isNotEmpty(descendant)) {
            ids.addAll(descendant.stream().map(Card::getId).collect(Collectors.toList()));
        }
        Map<Long, Map<String, Object>> cards = cardDao.findAsMap(ids, fields);
        if (MapUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<CardBean> cardBeans = cards.entrySet().stream()
                .map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())
                .collect(Collectors.toList());
        TotalBean<CardBean> totalBean = TotalBean.<CardBean>builder().total(cardBeans.size()).list(cardBeans).build();
        return totalBean;
    }

    public TotalBean<CardBean> searchUpDownCards(Card card, String[] fields) throws IOException {
        Long ancestorId = card.getParentId() > 0 ? card.getAncestorId() : card.getId();
        List<Card> tree = new ArrayList<>();
        Card ancestor = cardQueryService.select(ancestorId);
        if (null != ancestor) {
            tree.add(ancestor);
        }
        List<Card> descendant = cardQueryService.selectDescendant(ancestorId);
        if (CollectionUtils.isNotEmpty(descendant)) {
            tree.addAll(descendant);
        }
        Map<Long, Long> parentMap = tree.stream().collect(Collectors.toMap(Card::getId, Card::getParentId));
        List<Long> ids = new ArrayList<>();
        ids.add(card.getId());
        Long parentId = card.getParentId();
        while (parentId != null && parentId > 0) {
            ids.add(parentId);
            parentId = parentMap.get(parentId);
        }
        ids.addAll(descendantIds(card.getId(), tree));
        Map<Long, Map<String, Object>> cards = cardDao.findAsMap(ids, fields);
        if (MapUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<CardBean> cardBeans = cards.entrySet().stream()
                .map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())
                .collect(Collectors.toList());
        TotalBean<CardBean> totalBean = TotalBean.<CardBean>builder().total(cardBeans.size()).list(cardBeans).build();
        return totalBean;
    }

    private TotalBean<CardBean> searchCards(List<Card> cards, SearchEsRequest searchCardRequest) throws IOException {
        if (CollectionUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().total(0).build();
        }

        List<Long> matchIds = cardDao.searchIds(cards.stream().map(Card::getId).collect(Collectors.toList()), searchCardRequest.getQueries());
        Map<Long, Card> cardMap = cards.stream().collect(Collectors.toMap(Card::getId, Function.identity()));

        Map<Long, String> cardRanks = new HashMap<>();
        Set<Long> ancestorIds = new HashSet<>();
        matchIds.forEach(id -> {
            Card card = cardMap.get(id);
            cardRanks.put(card.getId(), card.getRank());
            if (card.getAncestorId() > 0) {
                ancestorIds.add(card.getAncestorId());
            } else {
                ancestorIds.add(card.getId());
            }
        });

        List<Long> allIds = new ArrayList<>(matchIds);
        if (CollectionUtils.isNotEmpty(ancestorIds)) {
            Set<Long> byPassIds = treeOtherIds(new HashSet<>(matchIds), ancestorIds);
            allIds.addAll(byPassIds);
            cardQueryService.select(new ArrayList<>(byPassIds)).forEach(card -> cardRanks.put(card.getId(), card.getRank()));
        }

        Map<Long, Map<String, Object>> allCards = cardDao.findAsMap(allIds, searchCardRequest.getFields());
        TotalBean<CardBean> totalBean = TotalBean.<CardBean>builder()
                .total(allCards.size())
                .list(allCards.entrySet().stream()
                        .map(e -> CardBean.builder()
                                .id(e.getKey())
                                .card(e.getValue())
                                .rank(cardRanks.get(e.getKey()))
                                .isByPassAncestor(!matchIds.contains(e.getKey()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
        referenceValues(totalBean, searchCardRequest.getFields());
        return totalBean;
    }

    private Set<Long> treeOtherIds(final Set<Long> ids, Set<Long> ancestorIds) {
        Set<Long> treeOtherIds = new HashSet<>(ancestorIds.stream()
                .filter(id -> !ids.contains(id))
                .collect(Collectors.toList()));
        cardQueryService.selectByAncestorIds(new ArrayList<>(ancestorIds)).stream()
                .filter(card -> !ids.contains(card.getId()))
                .forEach(card -> treeOtherIds.add(card.getId()));
        return treeOtherIds;
    }

    private Set<Long> byPassIds(final Set<Long> ids, final List<Card> cards, Set<Long> ancestorIds) {
        Set<Long> byPassIds = new HashSet<>();
        Map<Long, Long> parentIds = cardQueryService.selectByAncestorIds(new ArrayList<>(ancestorIds)).stream()
                .collect(Collectors.toMap(Card::getId, Card::getParentId));
        cards.stream().filter(card -> card.getAncestorId() > 0).forEach(card -> {
            Long parentId = card.getParentId();
            while (!ids.contains(parentId)) {
                byPassIds.add(parentId);
                parentId = parentIds.get(parentId);
            }
        });
        return byPassIds;
    }

    private List<Long> descendantIds(Long id, List<Card> cards) {
        Map<Long, List<Long>> childrenMap = cards.stream().collect(
                Collectors.groupingBy(Card::getParentId, Collectors.mapping(Card::getId, Collectors.toList())));
        List<Long> ids = new ArrayList<>();
        descendantIds(id, childrenMap, ids);
        return ids;
    }

    private void descendantIds(Long id, Map<Long, List<Long>> childrenMap, List<Long> descendantIds) {
        List<Long> children = childrenMap.get(id);
        if (CollectionUtils.isNotEmpty(children)) {
            descendantIds.addAll(children);
            children.forEach(child -> descendantIds(child, childrenMap, descendantIds));
        }
    }

    public TotalBean<CardBean> searchTitle(List<Long> cardIds) throws IOException {
        if (CollectionUtils.isEmpty(cardIds)) {
            return TotalBean.<CardBean>builder().build();
        }
        Map<Long, Map<String, Object>> cards = cardDao.findAsMap(cardIds, CardField.TITLE);
        if (MapUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<CardBean> cardBeans = cards.entrySet().stream()
                .map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())
                .collect(Collectors.toList());
        return TotalBean.<CardBean>builder().total(cardBeans.size()).list(cardBeans).build();
    }

    public List<Long> searchIdsThatNotEnd(List<Long> planIds) throws IOException {
        return cardDao.searchIds(
                In.builder().field(CardField.PLAN_ID).values(planIds.stream().map(String::valueOf).collect(Collectors.toList())).build(),
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build(),
                Eq.builder().field(CardField.CALC_IS_END).value(String.valueOf(false)).build()
        );
    }

    public List<Long> searchIdsThatIsEnd(List<Long> planIds) throws IOException {
        return cardDao.searchIds(
                In.builder().field(CardField.PLAN_ID).values(planIds.stream().map(String::valueOf).collect(Collectors.toList())).build(),
                Eq.builder().field(CardField.DELETED).value(String.valueOf(false)).build(),
                Eq.builder().field(CardField.CALC_IS_END).value(String.valueOf(true)).build()
        );
    }

    public long countNotEnd(Plan ancestorPlan) throws IOException {
        List<Long> planIds = new ArrayList<>();
        planIds.add(ancestorPlan.getId());
        List<Plan> descendants = planQueryService.selectDescendant(ancestorPlan, true);
        if (CollectionUtils.isNotEmpty(descendants)) {
            descendants.forEach(p -> planIds.add(p.getId()));
        }
        return cardDao.countNotEnd(planIds);
    }

    @NotNull
    public TotalBean<CardBean> selectDetail(List<Long> ids, boolean excludeDeleted, String... fields) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return TotalBean.<CardBean>builder().build();
        }
        if (excludeDeleted && !ArrayUtils.contains(fields, CardField.DELETED)) {
            fields = (String[]) ArrayUtils.add(fields, CardField.DELETED);
        }
        Map<Long, Map<String, Object>> cards = cardQueryService.selectDetail(ids, fields);
        if (MapUtils.isEmpty(cards)) {
            return TotalBean.<CardBean>builder().build();
        }
        List<CardBean> cardBeans = cards.entrySet().stream()
                .filter(e -> !(excludeDeleted && FieldUtil.getDeleted(e.getValue())))
                .map(e -> CardBean.builder().id(e.getKey()).card(e.getValue()).build())
                .collect(Collectors.toList());
        TotalBean<CardBean> totalBean = TotalBean.<CardBean>builder().total(cardBeans.size()).list(cardBeans).build();
        referenceValues(totalBean, fields);
        return totalBean;
    }

    @NotNull
    public TotalBean<CardBean> selectDetail(List<Long> ids, String... fields) throws IOException {
        return selectDetail(ids, false, fields);
    }

    @NotNull
    public TotalBean<CardBean> selectDetailByKeys(Long company, List<String> cardKeys, String... fields) throws IOException {
        return selectDetail(cardQueryService.selectIdsByKeys(company, cardKeys), fields);
    }

    @NotNull
    public TotalBean<CardBean> selectDetailByKeys(Long company, List<String> cardKeys, SearchEsRequest searchCardRequest, Boolean excludeDeleted, Integer pageNumber, Integer pageSize) throws IOException {
        List<Long> ids = cardQueryService.selectIdsByKeys(company, cardKeys);
        List<Query> queries = searchCardRequest.getQueries();
        if (queries == null) {
            queries = new ArrayList<>();
            searchCardRequest.setQueries(queries);
        }
        if (!excludeDeleted) {
            queries.add(Eq.builder().field(CardField.DELETED).value("false").build());
        }
        TotalBean<CardBean> totalBean = cardDao.search(ids, searchCardRequest, pageNumber, pageSize);

        if (totalBean.getTotal() > 0 && CollectionUtils.isEmpty(searchCardRequest.getSorts())) {
            //如果前端没有指定顺序，按照cardKeys中的顺序
            Map<Long, CardBean> source = new HashMap<>();
            for (CardBean cardBean : totalBean.getList()) {
                source.put(cardBean.getId(), cardBean);
            }
            List<CardBean> sortList = new ArrayList<>();
            for (Long id : ids) {
                CardBean cardBean = source.get(id);
                if (cardBean != null) {
                    sortList.add(cardBean);
                }
            }
        }
        referenceValues(totalBean, searchCardRequest.getFields());
        return totalBean;
    }

    private void referenceValues(TotalBean<CardBean> totalBean, String[] fields) throws IOException {
        ReferenceValues ref = new ReferenceValues();
        if (CollectionUtils.isEmpty(totalBean.getList()) || null == fields || fields.length == 0) {
            return;
        }
        List<Map<String, Object>> cards = totalBean.getList().stream().map(CardBean::getCard).collect(Collectors.toList());
        for (String field : fields) {
            referenceValueHelper.refCards(ref, field, cards);
        }
        totalBean.setRefs(ref);
    }

    public List<String> searchCardTypeByCompany(Long companyId, List<Query> queries) throws IOException {
        if (queries == null) {
            queries = new ArrayList<>();
        }
        queries.add(Eq.builder().field(CardField.COMPANY_ID).value(companyId.toString()).build());
        List<String> cardTypes = cardDao.searchCardTypes(queries);
        List<String> typeOrders = projectCardSchemaHelper.getSysProjectCardSchema().getOrderOfTypes();
        if (CollectionUtils.isNotEmpty(cardTypes)) {
            cardTypes = cardTypes.stream().sorted(Comparator.comparingInt(type -> typeOrders.indexOf(type))).collect(Collectors.toList());
        }
        return cardTypes;
    }
}
