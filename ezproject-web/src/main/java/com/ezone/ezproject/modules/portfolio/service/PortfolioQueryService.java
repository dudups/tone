package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioExample;
import com.ezone.ezproject.dal.mapper.ExtPortfolioMapper;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class PortfolioQueryService {
    private ExtPortfolioMapper portfolioMapper;
    private UserService userService;
    private CompanyService companyService;
    private PortfolioMemberQueryService portfolioMemberQueryService;

    /**
     * 查询子孙项目集，返回结果按层级顺序，层级深的在集合最后。
     *
     * @param portfolio
     * @return
     */
    public @NotNull List<Portfolio> selectDescendant(Portfolio portfolio) {
        PortfolioExample example = new PortfolioExample();
        example.createCriteria().andCompanyIdEqualTo(portfolio.getCompanyId()).andAncestorIdEqualTo(portfolio.getAncestorId());
        List<Portfolio> portfolios = portfolioMapper.selectByExample(example);
        Map<Long, List<Portfolio>> tree = portfolios.stream().collect(Collectors.groupingBy(Portfolio::getParentId));
        List<Portfolio> descendant = new ArrayList<>();
        collectDescendant(tree.get(portfolio.getId()), tree, descendant);
        return descendant;
    }

    /**
     * 查询子孙项目集，返回结果按层级顺序，层级深的在集合最后。
     *
     * @param portfolio
     * @return
     */
    public @NotNull List<Portfolio> selectCanReadReadDescendant(Portfolio portfolio) {
        List<Portfolio> portfolios = selectDescendant(portfolio);
        String user = userService.currentUserName();
        Long companyId = portfolio.getCompanyId();
        List<Long> portfolioIds = portfolioMemberQueryService.selectUserRolePortfolioIds(companyId, user);
        if (!userService.isCompanyAdmin(user,companyId)) {
            portfolios = portfolios.stream().filter(it -> portfolioIds.contains(it.getId())).collect(Collectors.toList());
        }
        return portfolios;
    }

    /**
     * 查询祖先节点
     *
     * @param portfolio
     * @return 返回portfolio的所有上层节点，根节点位于下标0，不包含参数本身
     */
    public @NotNull List<Portfolio> selectAncestor(Portfolio portfolio) {
        List<Portfolio> ancestor = new ArrayList<>();
        if (portfolio == null) {
            return ancestor;
        }
        PortfolioExample example = new PortfolioExample();
        example.createCriteria().andCompanyIdEqualTo(portfolio.getCompanyId()).andAncestorIdEqualTo(portfolio.getAncestorId());
        List<Portfolio> portfolios = portfolioMapper.selectByExample(example);
        Map<Long, Portfolio> tree = portfolios.stream().collect(Collectors.toMap(Portfolio::getId, Function.identity()));
        collectAncestor(tree.get(portfolio.getParentId()), tree, ancestor);
        return ancestor;
    }

    public Portfolio select(Long portfolioId) {
        if (portfolioId > 0) {
            Portfolio portfolio = portfolioMapper.selectByPrimaryKey(portfolioId);
            if (portfolio == null) {
                throw new CodedException(HttpStatus.NOT_FOUND, "未找到" + portfolioId);
            }
            return portfolio;
        }
        return null;
    }

    private void collectAncestor(Portfolio parent, Map<Long, Portfolio> tree, List<Portfolio> ancestor) {
        if (parent == null) {
            return;
        }
        collectAncestor(tree.get(parent.getParentId()), tree, ancestor);
        ancestor.add(parent);
    }

    private void collectDescendant(List<Portfolio> children, Map<Long, List<Portfolio>> tree, List<Portfolio> descendant) {
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        descendant.addAll(children);
        children.forEach(child -> collectDescendant(tree.get(child.getId()), tree, descendant));
    }


    public List<Portfolio> listAllOfCompany(Long companyId, String q) {
        PortfolioExample example = new PortfolioExample();
        PortfolioExample.Criteria criteria = example.createCriteria().andCompanyIdEqualTo(companyId);
        if (StringUtils.isNotEmpty(q)) {
            criteria.andNameLike(String.format("%%%s%%", q));
        }
        return portfolioMapper.selectByExample(example);
    }

    public List<Portfolio> searchRole(Long company, String user, String q) {
        if (userService.isCompanyAdmin(user, companyService.currentCompany())) {
            return listAllOfCompany(companyService.currentCompany(), q);
        } else {
            List<Long> ids = portfolioMemberQueryService.selectUserRolePortfolioIds(company, user);
            return selectWithIds(ids, q, "`id` ASC");
        }
    }

    public List<Portfolio> searchAdmin(Long company, String user, String q) {
        if (userService.isCompanyAdmin(user, company)) {
            return listAllOfCompany(companyService.currentCompany(), q);
        } else {
            List<Long> portfolioIds = portfolioMemberQueryService.selectAdminPortfolioIds(company, user);
            return selectWithIds(portfolioIds, q, "`id` ASC");
        }
    }

    public List<Portfolio> selectWithIds(List<Long> ids, String q, String sort) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.EMPTY_LIST;
        }
        PortfolioExample example = new PortfolioExample();
        PortfolioExample.Criteria criteria = example.createCriteria().andIdIn(ids);
        if (StringUtils.isNotEmpty(q)) {
            criteria.andNameLike(String.format("%%%s%%", q));
        }
        example.setOrderByClause(sort);
        return portfolioMapper.selectByExample(example);
    }

    public Long getPortfolioCompany(Long portfolioId) {
        Portfolio portfolio = portfolioMapper.selectByPrimaryKey(portfolioId);
        if (portfolio == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "项目集未找到");
        }
        return portfolio.getCompanyId();
    }

    public Map<Long, Portfolio> selectAncestor(List<Portfolio> portfolios, boolean excludeParamPortfolios) {
        Map<Long, Portfolio> result = new HashMap<>();
        if (CollectionUtils.isEmpty(portfolios)) {
            return result;
        }
        List<Long> ancestorIds = portfolios.stream().map(Portfolio::getAncestorId).distinct().collect(Collectors.toList());
        List<Portfolio> descendants = selectDescendant(ancestorIds);
        Map<Long, Portfolio> idPortfolioMap = descendants.stream().collect(Collectors.toMap(Portfolio::getId, portfolio -> portfolio));
        Set<Long> ids = portfolios.stream().map(Portfolio::getId).collect(Collectors.toSet());
        for (Portfolio portfolio : portfolios) {
            List<Portfolio> ancestors = new ArrayList<>();
            collectAncestor(idPortfolioMap.get(portfolio.getId()), idPortfolioMap, ancestors);
            if (CollectionUtils.isNotEmpty(ancestors)) {
                ancestors.forEach(ancestor -> {
                    if (excludeParamPortfolios && ids.contains(ancestor.getId())){
                        return;
                    }
                    result.putIfAbsent(ancestor.getId(), ancestor);
                });
            }
        }
        return result;
    }


    /**
     * 子孙卡片
     *
     * @param ancestorId 一级祖先卡片
     * @return
     */
    private List<Portfolio> selectDescendant(List<Long> ancestorId) {
        PortfolioExample example = new PortfolioExample();
        example.or().andAncestorIdIn(ancestorId);
        example.or().andIdIn(ancestorId);
        return portfolioMapper.selectByExample(example);
    }

}
