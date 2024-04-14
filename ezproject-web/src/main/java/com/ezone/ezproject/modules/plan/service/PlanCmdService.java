package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.PlanExample;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.PlanMapper;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.rank.RankLocation;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardHelper;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import com.ezone.ezproject.modules.card.service.CardSearchService;
import com.ezone.ezproject.modules.common.OperationContext;
import com.ezone.ezproject.modules.log.service.OperationLogCmdService;
import com.ezone.ezproject.modules.permission.PermissionService;
import com.ezone.ezproject.modules.plan.bean.CreatePlanRequest;
import com.ezone.ezproject.modules.plan.bean.CreatePlanTreeRequest;
import com.ezone.ezproject.modules.plan.bean.UpdatePlanRequest;
import com.ezone.ezproject.modules.plan.tree.TreeDeleteAndPromote;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PlanCmdService {
    private PlanMapper planMapper;

    private PlanQueryService planQueryService;

    private ProjectQueryService projectQueryService;

    private PermissionService permissionService;

    private UserService userService;

    private CardQueryService cardQueryService;

    private CardSearchService cardSearchService;

    private CardCmdService cardCmdService;

    private CardHelper cardHelper;

    private PlanGoalService planGoalService;

    private PlanSummaryService planSummaryService;
    private PlanNoticeService planNoticeService;

    private OperationLogCmdService operationLogCmdService;

    public Plan create(CreatePlanRequest request) throws IOException {
        String user = userService.currentUserName();
        Plan plan = CreatePlanHelper.builder()
                .user(user)
                .request(request)
                .planMapper(planMapper)
                .findDescendantPlan(p -> planQueryService.selectDescendant(p, true))
                .cardHelper(cardHelper)
                .build()
                .create();
        planNoticeService.saveOrUpdate(plan, true);
        return plan;
    }

    public Plan create(CreatePlanTreeRequest request) throws IOException {
        String user = userService.currentUserName();
        Plan plan = CreatePlanTreeHelper.builder()
                .user(user)
                .request(request)
                .planMapper(planMapper)
                .cardHelper(cardHelper)
                .build()
                .create();
        planNoticeService.saveOrUpdate(plan, true);
        operationLogCmdService.createSubPlan(OperationContext.instance(user), plan);
        return plan;
    }

    public void active(Plan plan) throws IOException {
        List<Long> planIds = new ArrayList<>();
        Map<Long, Plan> plans = MapUtils.EMPTY_MAP;
        if (plan.getAncestorId() > 0) {
            plans = planQueryService.selectTree(plan.getAncestorId(), false).stream().collect(Collectors.toMap(Plan::getId, p -> p));
        }
        Plan current = plan;
        // contains冗余判断以避免脏数据引起死循环
        while (current != null && !planIds.contains(current.getId())) {
            current.setIsActive(true);
            planMapper.updateByPrimaryKey(current);
            planIds.add(current.getId());
            current = plans.get(current.getParentId());
        }
        OperationContext opContext = OperationContext.instance(userService.currentUserName());
        cardCmdService.onPlanActive(opContext, plan.getProjectId(), planIds);
    }

    public Plan update(Long id, UpdatePlanRequest request) throws IOException {
        Plan plan = planQueryService.select(id);
        if (null == plan) {
            throw CodedException.NOT_FOUND;
        }
        String user = userService.currentUserName();
        OperationContext opContext = OperationContext.instance(user);
        if (!permissionService.isMember(user, plan.getProjectId())) {
            throw CodedException.FORBIDDEN;
        }
        Map<String, Object> updateContent = CreatePlanHelper.calculateUpdateContent(plan, request);
        plan.setName(request.getName());
        if (!plan.getParentId().equals(request.getParentId())) {
            if (request.getParentId() <= 0L) {
                plan.setParentId(0L);
                plan.setAncestorId(0L);
            } else {
                if (request.getParentId().equals(plan.getId())) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "父子层级循环！");
                }
                List<Plan> descendants = planQueryService.selectDescendant(plan, true);
                if (descendants.stream().anyMatch(p -> p.getId().equals(request.getParentId()))) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "父子层级循环！");
                }
                Plan parent = planQueryService.select(request.getParentId());
                if (BooleanUtils.isNotTrue(parent.getIsActive())) {
                    throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "不能设置父计划为非活跃计划！");
                }
                plan.setParentId(request.getParentId());
                plan.setAncestorId(parent.getAncestorId() > 0 ? parent.getAncestorId() : parent.getId());
            }
        }
        plan.setStartTime(request.getStartTime());
        plan.setEndTime(request.getEndTime());
        plan.setLastModifyUser(user);
        plan.setLastModifyTime(opContext.getTime());
        planMapper.updateByPrimaryKey(plan);
        planNoticeService.saveOrUpdate(plan, false);
        operationLogCmdService.updatePlan(opContext, plan, updateContent);
        return plan;
    }

    public void inactive(Plan plan, Long targetPlanId) throws IOException {
        List<Plan> plans = new ArrayList<>();
        plans.add(plan);
        List<Plan> descendants = planQueryService.selectDescendant(plan, true);
        if (CollectionUtils.isNotEmpty(descendants)) {
            plans.addAll(descendants);
        }
        List<Long> planIds = new ArrayList<>();
        String user = userService.currentUserName();
        Date now = new Date();
        plans.forEach(p -> {
            if (p.getId().equals(targetPlanId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Invalid targetPlanId!");
            }
            p.setIsActive(false);
            p.setLastModifyUser(user);
            p.setLastModifyTime(now);
            planMapper.updateByPrimaryKey(p);
            planIds.add(p.getId());
        });
        OperationContext operationContext = OperationContext.instance(user);
        cardCmdService.onPlanInActive(operationContext, plan.getProjectId(), planIds);
        List<Long> notEndCards = cardSearchService.searchIdsThatNotEnd(planIds);
        if (CollectionUtils.isNotEmpty(notEndCards)) {
            cardCmdService.migrate(cardQueryService.select(notEndCards), targetPlanId, null);
        }
    }

    public void delete(Plan plan, Long targetPlanId) throws IOException {
        List<Plan> plans = new ArrayList<>();
        plans.add(plan);
        List<Plan> descendants = planQueryService.selectDescendant(plan, null);
        if (CollectionUtils.isNotEmpty(descendants)) {
            plans.addAll(descendants);
        }
        List<Long> planIds = new ArrayList<>();
        plans.forEach(p -> {
            if (p.getId().equals(targetPlanId)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "Invalid targetPlanId!");
            }
            planIds.add(p.getId());
        });
        cardCmdService.migrate(cardQueryService.selectByPlanIds(planIds), targetPlanId, null);
        plans.forEach(p -> planMapper.deleteByPrimaryKey(p.getId()));
        planGoalService.deleteByPlanId(planIds);
        planNoticeService.delete(plan.getProjectId(), plans);
        operationLogCmdService.deletePlan(OperationContext.instance(userService.currentUserName()), plan);
    }

    public void deleteByProject(Long projectId) throws IOException {
        PlanExample example = new PlanExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        List<Plan> plans = planMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(plans)) {
            return;
        }
        planMapper.deleteByExample(example);
        List<Long> planIds = plans.stream().map(Plan::getId).collect(Collectors.toList());
        planGoalService.deleteByPlanId(planIds);
        planSummaryService.delete(planIds);
    }

    /**
     * 因需查询卡片/计划排序位置的前后相邻记录，故事务隔离级要求可重复读
     *
     * @return
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String updateDeliverLineRank(Plan plan, Long referenceCardId, RankLocation location) {
        Card referenceCard = cardQueryService.select(referenceCardId);
        String referenceRank = referenceCard.getRank();
        String deliverLineRank = cardHelper.ranks(plan.getProjectId(), referenceRank, location, 1).get(0);
        plan.setDeliverLineRank(deliverLineRank);
        planMapper.updateByPrimaryKey(plan);
        return deliverLineRank;
    }

    public void cleanInactivePlan(Long projectId) throws IOException {
        Project project = projectQueryService.select(projectId);
        if (project == null) {
            return;
        }
        if (project.getPlanKeepDays() <= 0) {
            return;
        }
        List<Plan> plans = planQueryService.selectByProjectId(projectId, null);
        if (CollectionUtils.isEmpty(plans)) {
            return;
        }
        Date expireDate = DateUtils.addDays(new Date(), -project.getPlanKeepDays().intValue());
        Function<Plan, Boolean> isExpired = plan -> BooleanUtils.isNotTrue(plan.getIsActive()) && plan.getLastModifyTime().before(expireDate);
        List<Long> planIds = TreeDeleteAndPromote.builder()
                .planMapper(planMapper)
                .plans(plans)
                .isExpired(isExpired)
                .build()
                .run();
        List<Card> cards = cardQueryService.selectAllByPlanIds(planIds);
        if (CollectionUtils.isEmpty(cards)) {
            return;
        }
        planIds.forEach(planMapper::deleteByPrimaryKey);
        cardCmdService.delete(project.getCompanyId(), cards.stream().map(Card::getId).collect(Collectors.toList()));
    }
}
