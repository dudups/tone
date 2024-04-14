package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.bean.TotalBean;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.entity.ProjectExample;
import com.ezone.ezproject.dal.entity.RelPortfolioProjectExample;
import com.ezone.ezproject.dal.mapper.ExtPortfolioMapper;
import com.ezone.ezproject.dal.mapper.ProjectDomainMapper;
import com.ezone.ezproject.dal.mapper.ProjectMapper;
import com.ezone.ezproject.dal.mapper.RelPortfolioProjectMapper;
import com.ezone.ezproject.es.dao.ProjectExtendDao;
import com.ezone.ezproject.es.entity.ProjectField;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.Eq;
import com.ezone.ezproject.modules.card.bean.query.Ids;
import com.ezone.ezproject.modules.card.bean.query.Keyword;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.project.bean.ProjectExt;
import com.ezone.ezproject.modules.project.bean.SearchScope;
import com.github.pagehelper.PageHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectQueryService {
    private ProjectMapper projectMapper;
    private CompanyService companyService;
    private UserService userService;

    private ProjectMemberQueryService memberQueryService;
    private ProjectFavouriteQueryService favouriteQueryService;

    private ProjectExtendDao projectExtendDao;

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectQueryService.select"},
            key = "#id",
            unless = "#result == null"
    )
    public Project select(Long id) {
        return projectMapper.selectByPrimaryKey(id);
    }

    public List<Project> select(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ListUtils.EMPTY_LIST;
        }
        ProjectExample example = new ProjectExample();
        example.createCriteria().andIdIn(ids);
        return projectMapper.selectByExample(example);
    }

    public Map<String, Object> selectExtend(Long id) throws IOException {
        return projectExtendDao.find(id);
    }

    public List<Map<String, Object>> selectExtend(List<Long> ids) throws IOException {
        return projectExtendDao.find(ids);
    }

    public long countByCompany(List<Long> companyIds) {
        ProjectExample example = new ProjectExample();
        if (CollectionUtils.isNotEmpty(companyIds)) {
            example.createCriteria().andCompanyIdIn(companyIds);
        }
        return projectMapper.countByExample(example);
    }

    public List<Project> select(Long companyId, List<String> projectKeys) {
        ProjectExample example = new ProjectExample();
        example.createCriteria().andCompanyIdEqualTo(companyId).andKeyIn(projectKeys);
        return projectMapper.selectByExample(example);
    }

    public List<Project> selectAll(Integer pageNumber, Integer pageSize) {
        ProjectExample example = new ProjectExample();
        PageHelper.startPage(pageNumber, pageSize, true);
        return projectMapper.selectByExample(example);
    }

    public Project select(String key) {
        Long company = companyService.currentCompany();
        ProjectExample example = new ProjectExample();
        example.createCriteria().andCompanyIdEqualTo(company).andKeyEqualTo(key);
        List<Project> projects = projectMapper.selectByExample(example);
        if (CollectionUtils.isEmpty(projects)) {
            return null;
        }
        return projects.get(0);
    }

    public TotalBean<ProjectExt> search(
            Long company, String user,
            SearchScope scope,
            String q,
            Integer pageNumber, Integer pageSize
    ) throws IOException {
        SearchEsRequest searchEsRequest = SearchEsRequest.builder()
                .queries(Arrays.asList(Keyword.builder().fields(Arrays.asList(ProjectField.KEY, ProjectField.NAME)).values(q).build()))
                .sorts(Arrays.asList(SearchEsRequest.Sort.builder().field(ProjectField.TOP_SCORE).order(SortOrder.DESC).build()))
                .build();
        return search(company, user, scope, searchEsRequest, pageNumber, pageSize);
    }

    public TotalBean<ProjectExt> search(
            SearchScope scope,
            SearchEsRequest searchEsRequest,
            Integer pageNumber, Integer pageSize
    ) throws IOException {
        Long company = companyService.currentCompany();
        if (company == null) {
            return TotalBean.builder().total(0L).list(ListUtils.EMPTY_LIST).build();
        }
        String user = userService.currentUserName();
        return search(company, user, scope, searchEsRequest, pageNumber, pageSize);
    }

    public TotalBean<ProjectExt> search(
            Long company, String user,
            SearchScope scope,
            SearchEsRequest searchEsRequest,
            Integer pageNumber, Integer pageSize
    ) throws IOException {
        List<Long> favouriteIds = favouriteQueryService.selectFavouriteProjectIds(company, user);
        Function<Long, Boolean> isFavorite = favouriteIds::contains;
        boolean limitScope = false;
        Supplier<List<Long>> scopeIds = null;
        boolean isAdmin = userService.isCompanyAdmin(user, company);
        switch (scope) {
            case ADMIN:
                if (!isAdmin) {
                    limitScope = true;
                    scopeIds = () -> memberQueryService.selectAdminProjectIds(company, user);
                }
                break;
            case ROLE:
                if (!isAdmin) {
                    limitScope = true;
                    scopeIds = () -> memberQueryService.selectUserRoleProjectIds(company, user);
                }
                break;
            case MEMBER:
                if (!isAdmin) {
                    limitScope = true;
                    scopeIds = () -> memberQueryService.selectUserMemberProjectIds(company, user);
                }
                break;
            case FAVOURITE:
                limitScope = true;
                scopeIds = () -> favouriteIds;
                isFavorite = project -> true;
                break;
            default:
                limitScope = false;
                break;
        }
        TotalBean<ProjectExt> total = searchScope(company, searchEsRequest, limitScope, scopeIds, pageNumber, pageSize);
        if (CollectionUtils.isEmpty(total.getList())) {
            return total;
        }
        List<Long> ids = total.getList().stream().map(ProjectExt::getId).collect(Collectors.toList());
        Map<Long, Project> projects = select(ids).stream().collect(Collectors.toMap(Project::getId, Function.identity()));
        for (ProjectExt projectExt : total.getList()) {
            projectExt.set(projects.get(projectExt.getId()), isFavorite.apply(projectExt.getId()));
        }
        return total;
    }

    private TotalBean<ProjectExt> searchScope(
            Long company,
            SearchEsRequest searchEsRequest,
            boolean limitScope, Supplier<List<Long>> scopeIds,
            Integer pageNumber, Integer pageSize) throws IOException {
        List<Query> queries = searchEsRequest.getQueries();
        List<Query> finalQueries = new ArrayList<>();
        finalQueries.add(Eq.builder().field(ProjectField.COMPANY_ID).value(String.valueOf(company)).build());
        if (CollectionUtils.isNotEmpty(queries)) {
            finalQueries.addAll(queries);
        }
        if (limitScope) {
            List<Long> ids = scopeIds.get();
            if (CollectionUtils.isEmpty(ids)) {
                return TotalBean.<ProjectExt>builder().build();
            }
            finalQueries.add(Ids.builder().ids(ids.stream().map(String::valueOf).collect(Collectors.toList())).build());
        }
        searchEsRequest.setQueries(finalQueries);
        return projectExtendDao.search(searchEsRequest, pageNumber, pageSize);
    }

    @Cacheable(
            cacheManager = CacheManagers.LOCAL_CACHE_MANAGER,
            cacheNames = "ProjectService.getProjectCompany",
            key = "#projectId",
            unless = "#result == null"
    )
    public Long getProjectCompany(Long projectId) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        if (null == project) {
            throw CodedException.NOT_FOUND;
        }
        return project.getCompanyId();
    }

    public Collection<Long> filterRead(String user, List<Long> projectIds, Long companyId) {
        if (CollectionUtils.isEmpty(projectIds)) {
            return Collections.emptyList();
        }

        if (userService.isCompanyAdmin(user, companyId)) {
            return projectIds;
        }

        List<Long> canReadProjectIds = memberQueryService.selectUserRoleProjectIds(companyId, user);
        if (CollectionUtils.isEmpty(canReadProjectIds)) {
            return Collections.emptyList();
        }
        return CollectionUtils.intersection(canReadProjectIds, projectIds);
    }

    public void checkKey(String key, Long excludeId) {
        ProjectExample example = new ProjectExample();
        ProjectExample.Criteria criteria = example.createCriteria()
                .andCompanyIdEqualTo(companyService.currentCompany())
                .andKeyEqualTo(key);
        if (excludeId != null && excludeId > 0) {
            criteria.andIdNotEqualTo(excludeId);
        }
        if (projectMapper.countByExample(example) > 0) {
            throw new CodedException(ErrorCode.KEY_CONFLICT, "标识冲突!");
        }
    }

    public void checkKey(String key) {
        checkKey(key, null);
    }

    public void checkName(String name, Long excludeId) {
        ProjectExample example = new ProjectExample();
        ProjectExample.Criteria criteria = example.createCriteria()
                .andCompanyIdEqualTo(companyService.currentCompany())
                .andNameEqualTo(name);
        if (excludeId != null && excludeId > 0) {
            criteria.andIdNotEqualTo(excludeId);
        }
        if (projectMapper.countByExample(example) > 0) {
            throw new CodedException(ErrorCode.NAME_CONFLICT, "名字冲突!");
        }
    }

    public void checkName(String key) {
        checkName(key, null);
    }

}
