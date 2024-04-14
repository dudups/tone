package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.RelPortfolioProject;
import com.ezone.ezproject.dal.entity.RelPortfolioProjectExample;
import com.ezone.ezproject.dal.mapper.RelPortfolioProjectMapper;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Gt;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.chart.service.ChartDataService;
import com.ezone.ezproject.modules.chart.service.PlanDataService;
import com.ezone.ezproject.modules.permission.PermissionService;
import com.ezone.ezproject.modules.plan.bean.PlansAndProgresses;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioAndSummaryInfo;
import com.ezone.ezproject.modules.portfolio.bean.PortfolioProjectPlan;
import com.ezone.ezproject.modules.portfolio.bean.ProjectCardSummary;
import com.ezone.ezproject.modules.portfolio.bean.SearchScope;
import com.ezone.ezproject.modules.portfolio.bean.TotalPortfolioInfo;
import com.ezone.ezproject.modules.portfolio.bean.TotalPortfolioProjectSummary;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class RelPortfolioProjectService {
    private RelPortfolioProjectMapper relPortfolioProjectMapper;
    private ProjectQueryService projectQueryService;
    private PortfolioQueryService portfolioQueryService;
    private PlanQueryService planQueryService;
    private UserService userService;
    private CompanyService companyService;
    private ChartDataService chartDataService;
    private PlanDataService planDataService;
    private PortfolioFavouriteQueryService portfolioFavouriteQueryService;
    private PermissionService permissionService;

    /**
     * 覆盖更新
     * @param portfolioId
     * @param projectIds
     */
    public void updateBindProjects(Long portfolioId, List<Long> projectIds) {
        if (CollectionUtils.isEmpty(projectIds)) {
            RelPortfolioProjectExample example = new RelPortfolioProjectExample();
            example.createCriteria().andPortfolioIdEqualTo(portfolioId);
            relPortfolioProjectMapper.deleteByExample(example);
            return;
        }
        List<Project> projects = projectQueryService.select(projectIds);
        if (CollectionUtils.isEmpty(projects) || projects.size() < projectIds.size()) {
            throw new CodedException(HttpStatus.NOT_FOUND, "未找到项目");
        }
        List<RelPortfolioProject> relProjects = select(portfolioId);
        List<Long> addProjectIds = new ArrayList<>(projectIds);
        if (CollectionUtils.isNotEmpty(relProjects)) {
            List<Long> hasRelProjectIds = projectIds.stream().filter(projectId -> relProjects.stream()
                            .anyMatch(relPortfolioProject -> relPortfolioProject.getProjectId().equals(projectId)))
                    .collect(Collectors.toList());
            addProjectIds.removeAll(hasRelProjectIds);
        }
        Map<Long, Project> projectMap = projects.stream().collect(Collectors.toMap(Project::getId, Function.identity(), (project1, project2) -> project2));
        if (CollectionUtils.isNotEmpty(addProjectIds)) {
            addProjectIds.forEach(projectId -> {
                UserProjectPermissions permissions = permissionService.permissions(userService.currentUserName(), projectId);
                if (permissions == null || !permissions.hasPermission(OperationType.PROJECT_READ)) {
                    throw new CodedException(HttpStatus.FORBIDDEN, String.format("无[%s]项目权限！", projectMap.get(projectId).getName()));
                }
            });
        }

        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andPortfolioIdEqualTo(portfolioId);
        relPortfolioProjectMapper.deleteByExample(example);
        projectIds.forEach(projectId -> {
            RelPortfolioProject relProject = RelPortfolioProject.builder()
                    .id(IdUtil.generateId())
                    .companyId(projectMap.get(projectId).getCompanyId())
                    .projectId(projectId)
                    .portfolioId(portfolioId)
                    .createUser(userService.currentUserName())
                    .createTime(new Date())
                    .build();
            relPortfolioProjectMapper.insert(relProject);
        });
    }

    public void removeProject(Long portfolioId, Long projectId) {
        Project project = projectQueryService.select(projectId);
        if (project == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "未找到项目");
        }
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andCompanyIdEqualTo(companyService.currentCompany())
                .andPortfolioIdEqualTo(portfolioId)
                .andProjectIdEqualTo(projectId);
        relPortfolioProjectMapper.deleteByExample(example);
    }

    public void deletePortfolio(Long portfolioId) {
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andCompanyIdEqualTo(companyService.currentCompany())
                .andPortfolioIdEqualTo(portfolioId);
        relPortfolioProjectMapper.deleteByExample(example);
    }

    public TotalPortfolioProjectSummary listProjectAndSummary(Long portfolioId, Boolean containSubPortfolio, Boolean excludeNoPlan) {
        TotalPortfolioProjectSummary totalPortfolioProject = new TotalPortfolioProjectSummary();
        List<Long> portfolioIds = new ArrayList<>();
        List<Portfolio> all = new ArrayList<>();
        Portfolio portfolio = portfolioQueryService.select(portfolioId);
        all.add(portfolio);
        if (Boolean.TRUE.equals(containSubPortfolio)) {
            List<Portfolio> subPortfolios = portfolioQueryService.selectCanReadReadDescendant(portfolio);
            subPortfolios.forEach(subPortfolio -> portfolioIds.add(subPortfolio.getId()));
            all.addAll(subPortfolios);
            totalPortfolioProject.setPortfolios(all);
        }

        portfolioIds.add(portfolioId);
        List<RelPortfolioProject> relProjects = select(portfolioIds);
        if (CollectionUtils.isNotEmpty(relProjects)) {
            List<Long> projectIds = relProjects.stream().map(RelPortfolioProject::getProjectId).collect(Collectors.toList());
            List<Project> projects = projectQueryService.select(projectIds);
            if (CollectionUtils.isNotEmpty(projects)) {
                totalPortfolioProject.setProjects(projects.stream().collect(Collectors.toMap(Project::getId, Function.identity())));
            }
            Map<Long, Long> plansCount = planQueryService.plansCount(projectIds);
            List<Query> queries = new ArrayList<>();
            queries.add(Eq.builder().field(CardField.DELETED).value("false").build());
            if (BooleanUtils.isTrue(excludeNoPlan)) {
                queries.add(Gt.builder().field(CardField.PLAN_ID).value("0").build());
            }
            Map<Long, ProjectCardSummary> projectCardSummaryMap = chartDataService.projectCardsSummary(projectIds, queries);
            plansCount.forEach((id, planCount) -> {
                ProjectCardSummary projectCardSummary = projectCardSummaryMap.get(id);
                if (projectCardSummary == null) {
                    projectCardSummary = ProjectCardSummary.builder().projectId(id).build();
                    projectCardSummaryMap.put(id, projectCardSummary);
                }
                projectCardSummary.setPlanCount(planCount);
            });
            totalPortfolioProject.setProjectCardSummaryMap(projectCardSummaryMap);
            totalPortfolioProject.setRelPortfolioProjects(relProjects.stream().collect(Collectors.groupingBy(RelPortfolioProject::getPortfolioId)));
        }
        return totalPortfolioProject;
    }

    public List<Long> queryRelationProjectIds(Long portfolioId, Boolean containSubPortfolio) {
        List<Long> portfolioIds = new ArrayList<>();
        if (Boolean.TRUE.equals(containSubPortfolio)) {
            Portfolio portfolio = portfolioQueryService.select(portfolioId);
            List<Portfolio> subPortfolios = portfolioQueryService.selectDescendant(portfolio);
            subPortfolios.forEach(subPortfolio -> portfolioIds.add(subPortfolio.getId()));
        }
        portfolioIds.add(portfolioId);
        List<RelPortfolioProject> relProjects = select(portfolioIds);
        List<Long> projectIds;
        if (CollectionUtils.isNotEmpty(relProjects)) {
            projectIds = relProjects.stream().map(RelPortfolioProject::getProjectId).collect(Collectors.toList());
        } else {
            projectIds = Collections.emptyList();
        }
        return projectIds;
    }

    public TotalPortfolioInfo listPortfolio(String q, SearchScope scope, boolean showCompleteTree) {
        Long company = companyService.currentCompany();
        if (company == null) {
            return TotalPortfolioInfo.builder().list(ListUtils.EMPTY_LIST).build();
        }
        String user = userService.currentUserName();
        List<Long> favouriteIds = portfolioFavouriteQueryService.selectFavouritePortfolioIds(company, user);
        Function<Long, Boolean> isFavorite = favouriteIds::contains;
        List<Portfolio> portfolios = null;
        switch (scope) {
            case ADMIN:
                portfolios = portfolioQueryService.searchAdmin(company, user, q);
                break;
            case FAVOURITE:
                portfolios = portfolioQueryService.selectWithIds(favouriteIds, q, "`id` ASC");
                isFavorite = project -> true;
                break;
            case ROLE:
            default:
                portfolios = portfolioQueryService.searchRole(company, user, q);
                break;
        }
        if (CollectionUtils.isEmpty(portfolios)) {
            return TotalPortfolioInfo.builder().list(ListUtils.EMPTY_LIST).build();
        }
        final Function<Long, Boolean> isFavoriteFunc = isFavorite;
        List<Long> portfolioIds = portfolios.stream().map(Portfolio::getId).collect(Collectors.toList());
        Map<Long, List<Long>> portfolioRelProjectIds = queryRelationProjectIds(portfolioIds);
        Function<Long, Long> projectCountFunc = (Long portfolioId) -> portfolioRelProjectIds.get(portfolioId) == null ? 0L : portfolioRelProjectIds.get(portfolioId).size();
        List<Long> projectIds = portfolioRelProjectIds.values().stream().flatMap(List::stream).distinct().collect(Collectors.toList());
        Map<Long, Long> projectPlansCount = planQueryService.plansCount(projectIds);
        Map<Long, Long> portfolioRelPlanCount = new HashMap<>();
        portfolioRelProjectIds.forEach((portfolioId, relProjectIds) -> {
            long count = portfolioRelPlanCount.getOrDefault(portfolioId, 0L);
            for (Long projectId : relProjectIds) {
                Long planCount = projectPlansCount.getOrDefault(projectId, 0L);
                count = count + planCount;
            }
            portfolioRelPlanCount.put(portfolioId, count);
        });
        Function<Long, Long> planCountFunc = (Long portfolioId) -> portfolioRelPlanCount.get(portfolioId) == null ? 0L : portfolioRelPlanCount.get(portfolioId);
        List<PortfolioAndSummaryInfo> portfolioInfos = portfolios.stream()
                .map(portfolio -> PortfolioAndSummaryInfo.builder().portfolio(portfolio)
                        .favorite(isFavoriteFunc.apply(portfolio.getId()))
                        .projectCount(projectCountFunc.apply(portfolio.getId()))
                        .planCount(planCountFunc.apply(portfolio.getId()))
                        .build())
                .collect(Collectors.toList());
        TotalPortfolioInfo.TotalPortfolioInfoBuilder result = TotalPortfolioInfo.builder().list(portfolioInfos);
        if (showCompleteTree) {
            Map<Long, Portfolio> ancestors = portfolioQueryService.selectAncestor(portfolios, true);
            result.ancestors(ancestors);
        }
        return result.build();

    }

    public PortfolioProjectPlan getProjectActivePlansProgress(Long projectId) throws IOException {
        List<Plan> plans = planQueryService.selectByProjectId(projectId, true);
        Map<Long, PlansAndProgresses.Progress> planProgresses = planDataService.planProgress(plans.stream().map(Plan::getId).collect(Collectors.toList()));
        return PortfolioProjectPlan.builder().plans(plans).planProgressMap(planProgresses).build();
    }

    /**
     * 查询项目集关联的项目ID。key-项目集ID, value-关联的项目ID
     *
     * @param portfolioIds
     * @return
     */
    private Map<Long, List<Long>> queryRelationProjectIds(List<Long> portfolioIds) {
        List<RelPortfolioProject> relProjects = select(portfolioIds);
        Map<Long, List<Long>> projectIds;
        if (CollectionUtils.isNotEmpty(relProjects)) {
            projectIds = relProjects.stream().collect(Collectors.groupingBy(RelPortfolioProject::getPortfolioId, Collectors.mapping(RelPortfolioProject::getProjectId, Collectors.toList())));
        } else {
            projectIds = new HashMap<>();
        }
        return projectIds;
    }

    private List<RelPortfolioProject> select(Long portfolioId, Long projectId) {
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andCompanyIdEqualTo(companyService.currentCompany())
                .andPortfolioIdEqualTo(portfolioId)
                .andProjectIdEqualTo(projectId);
        return relPortfolioProjectMapper.selectByExample(example);
    }

    public List<RelPortfolioProject> select(List<Long> portfolioIds) {
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andCompanyIdEqualTo(companyService.currentCompany())
                .andPortfolioIdIn(portfolioIds);
        example.setOrderByClause("id desc");
        return relPortfolioProjectMapper.selectByExample(example);
    }

    private List<RelPortfolioProject> select(Long portfolioId) {
        RelPortfolioProjectExample example = new RelPortfolioProjectExample();
        example.createCriteria().andCompanyIdEqualTo(companyService.currentCompany())
                .andPortfolioIdEqualTo(portfolioId);
        example.setOrderByClause("id desc");
        return relPortfolioProjectMapper.selectByExample(example);
    }
}
