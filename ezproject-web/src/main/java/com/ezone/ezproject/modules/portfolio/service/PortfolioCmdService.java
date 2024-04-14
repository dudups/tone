package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioExample;
import com.ezone.ezproject.dal.mapper.ExtPortfolioMapper;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.chart.config.range.DateBetweenRange;
import com.ezone.ezproject.modules.portfolio.bean.CreatePortfolioRequest;
import com.ezone.ezproject.modules.portfolio.bean.EditPortfolioRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioCmdService {
    @Value("${portfolio.limit.maxLayer:6}")
    private int maxPortfolioLayer;
    private final ExtPortfolioMapper portfolioMapper;
    private final PortfolioQueryService portfolioQueryService;
    private final CompanyService companyService;
    private final UserService userService;
    private final PortfolioMemberCmdService portfolioMemberCmdService;
    private final PortfolioSchemaCmdService portfolioSchemaCmdService;
    private final PortfolioChartCmdService portfolioChartCmdService;
    private final RelPortfolioProjectService relPortfolioProjectService;

    public Portfolio create(CreatePortfolioRequest createPortfolioRequest) {
        CreatePortfolioRequest.PortfolioBean crateReq = createPortfolioRequest.getPortfolio();
        List<CreatePortfolioRequest.PortfolioBean> subPortfolioCreateReq = crateReq.getSubPortfolios();
        Long parentId = createPortfolioRequest.getParentId();
        Portfolio parentPortfolio = portfolioQueryService.select(parentId);
        List<Portfolio> ancestor = portfolioQueryService.selectAncestor(parentPortfolio);
        DateBetweenRange minRange = getMinRange(parentPortfolio, ancestor);
        checkPortfolioDate(crateReq, minRange.getStart(), minRange.getEnd());
        checkPortfolioMaxLayer(ancestor, createPortfolioRequest);
        Long id = IdUtil.generateId();
        String path = getPath(id, parentPortfolio);
        String user = userService.currentUserName();
        Long ancestorId = getAncestorId(id, parentPortfolio);
        Portfolio portfolio = Portfolio.builder().id(id).name(crateReq.getName()).startDate(crateReq.getStartDate()).endDate(crateReq.getEndDate()).companyId(companyService.currentCompany()).parentId(parentId).ancestorId(ancestorId).path(path).build();
        portfolioMapper.insert(portfolio);
        portfolioMemberCmdService.initMembers(portfolio, user);
        portfolioSchemaCmdService.setPortfolioRoleSchema(portfolio.getId());
        portfolioChartCmdService.initCharts(portfolio, user);
        createSubPortfolios(portfolio, subPortfolioCreateReq, user, ancestorId);
        return portfolio;
    }

    private DateBetweenRange getMinRange(Portfolio portfolio, List<Portfolio> parentAncestor) {
        DateBetweenRange range = new DateBetweenRange();
        if (portfolio == null) {
            return range;
        }
        Date maxStart = portfolio.getStartDate();
        Date minEmd = portfolio.getEndDate();
        if (CollectionUtils.isNotEmpty(parentAncestor)) {
            for (Portfolio parent : parentAncestor) {
                Date startDate = parent.getStartDate();
                if (startDate != null && (maxStart == null || startDate.getTime() > maxStart.getTime())) {
                    maxStart = startDate;
                }
                Date endDate = parent.getEndDate();
                if (endDate != null && (minEmd == null || endDate.getTime() < minEmd.getTime())) {
                    minEmd = endDate;
                }
            }
        }
        range.setStart(maxStart);
        range.setEnd(minEmd);
        return range;
    }

    /**
     * 创建子项目集
     *
     * @param portfolio
     * @param subPortfolioCreateReq
     * @param user
     * @param ancestorId
     */
    private void createSubPortfolios(Portfolio portfolio, List<CreatePortfolioRequest.PortfolioBean> subPortfolioCreateReq, String user, Long ancestorId) {
        if (CollectionUtils.isEmpty(subPortfolioCreateReq)) {
            return;
        }
        for (CreatePortfolioRequest.PortfolioBean portfolioBean : subPortfolioCreateReq) {
            Long subId = IdUtil.generateId();
            Portfolio subPortfolio = Portfolio.builder().id(subId).name(portfolioBean.getName()).startDate(portfolioBean.getStartDate()).endDate(portfolioBean.getEndDate()).companyId(companyService.currentCompany()).parentId(portfolio.getId()).ancestorId(ancestorId).path(getPath(subId, portfolio.getPath())).build();
            portfolioMapper.insert(subPortfolio);
            portfolioMemberCmdService.initMembers(subPortfolio, user);
            portfolioSchemaCmdService.setPortfolioRoleSchema(subPortfolio.getId());
            portfolioChartCmdService.initCharts(subPortfolio, user);
            createSubPortfolios(subPortfolio, portfolioBean.getSubPortfolios(), user, ancestorId);
        }
    }

    private void checkPortfolioDate(CreatePortfolioRequest.PortfolioBean portfolioBean, Date maxStart, Date minEnd) {
        Date startDate = portfolioBean.getStartDate();
        Date endDate = portfolioBean.getEndDate();
        if (endDate != null) {
            if (minEnd == null || endDate.getTime() < minEnd.getTime()) {
                minEnd = endDate;
            }
            if (endDate.getTime() > minEnd.getTime()) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "子项目集时间范围要在父项目集之内！");
            }
        }
        if (startDate != null) {
            if (maxStart == null || startDate.getTime() > maxStart.getTime()) {
                maxStart = startDate;
            }
            if (startDate.getTime() < maxStart.getTime()) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "子项目集时间范围要在父项目集之内！");
            }
        }
        List<CreatePortfolioRequest.PortfolioBean> subPortfolioCreateReq = portfolioBean.getSubPortfolios();
        if (CollectionUtils.isNotEmpty(subPortfolioCreateReq)) {
            for (CreatePortfolioRequest.PortfolioBean subPortfolioBean : subPortfolioCreateReq) {
                checkPortfolioDate(subPortfolioBean, maxStart, minEnd);
            }
        }
    }

    private void checkPortfolioDate(Portfolio parentPortfolio, Date start, Date end) {
        if (parentPortfolio == null) {
            return;
        }
        List<Portfolio> portfolios = portfolioQueryService.selectAncestor(parentPortfolio);
        DateBetweenRange minRange = getMinRange(parentPortfolio, portfolios);
        if (minRange.getStart() != null && start != null && start.before(minRange.getStart())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "子项目集时间范围要在父项目集之内！");
        }
        if (minRange.getEnd() != null && end != null && end.after(minRange.getEnd())) {
            throw new CodedException(HttpStatus.NOT_ACCEPTABLE, "子项目集时间范围要在父项目集之内！");
        }
    }

    private Long getAncestorId(Long id, Portfolio parentPortfolio) {
        if (parentPortfolio != null) {
            return parentPortfolio.getAncestorId();
        }
        return id;
    }

    private String getPath(Long id, Portfolio parentPortfolio) {
        String path;
        if (parentPortfolio != null) {
            String parentPath = parentPortfolio.getPath();
            path = parentPath + "/" + id;
        } else {
            path = String.valueOf(id);
        }
        return path;
    }

    private String getPath(Long id, String parentPath) {
        String path;
        if (StringUtils.isNotEmpty(parentPath)) {
            path = parentPath + "/" + id;
        } else {
            path = String.valueOf(id);
        }
        return path;
    }

    public void delete(Long id) {
        Portfolio portfolio = portfolioMapper.selectByPrimaryKey(id);
        List<Portfolio> subPortfolios = portfolioQueryService.selectDescendant(portfolio);
        if (CollectionUtils.isNotEmpty(subPortfolios)) {
            subPortfolios.forEach(subPortfolio -> doDelete(subPortfolio.getId()));
        }
        doDelete(id);
    }

    private void doDelete(Long id) {
        portfolioMapper.deleteByPrimaryKey(id);
        portfolioMemberCmdService.deleteByPortfolioId(id);
        portfolioSchemaCmdService.delete(id);
        portfolioChartCmdService.deleteCharts(id);
        relPortfolioProjectService.deletePortfolio(id);
    }

    public void edit(Long id, EditPortfolioRequest editRequest) {
        Long parentId = editRequest.getParentId();
        Portfolio parentPortfolio = portfolioQueryService.select(parentId);
        checkPortfolioDate(parentPortfolio, editRequest.getStartDate(), editRequest.getEndDate());
        Long ancestorId = getAncestorId(id, parentPortfolio);

        Portfolio portfolio = portfolioQueryService.select(id);
        String oldName = portfolio.getName();
        if (!StringUtils.equals(oldName, editRequest.getName())) {
            checkPortfolioNames(portfolio.getCompanyId(), Arrays.asList(editRequest.getName()));
        }
        Long oldParentId = portfolio.getParentId();
        List<Portfolio> existSubPortfolio = null;
        if (!Objects.equals(oldParentId, parentId)) {
            existSubPortfolio = portfolioQueryService.selectDescendant(portfolio);
            portfolio.setParentId(parentId);
            portfolio.setAncestorId(ancestorId);
            String path = getPath(id, parentPortfolio);
            portfolio.setPath(path);
        }
        portfolio.setName(editRequest.getName());
        portfolio.setStartDate(editRequest.getStartDate());
        portfolio.setEndDate(editRequest.getEndDate());
        portfolioMapper.updateByPrimaryKeySelective(portfolio);
        if (CollectionUtils.isNotEmpty(existSubPortfolio)) {
            Map<Long, Portfolio> portfolioMap = existSubPortfolio.stream().collect(Collectors.toMap(Portfolio::getId, Function.identity()));
            portfolioMap.put(portfolio.getId(), portfolio);
            for (Portfolio subPortfolio : existSubPortfolio) {
                subPortfolio.setPath(getPath(subPortfolio.getId(), portfolioMap.get(subPortfolio.getParentId())));
                subPortfolio.setAncestorId(ancestorId);
                portfolioMapper.updateByPrimaryKeySelective(subPortfolio);
            }
        }
    }

    public void checkPortfolioNames(CreatePortfolioRequest request) {
        List<String> reqNames = new ArrayList<>();
        CreatePortfolioRequest.PortfolioBean portfolioBean = request.getPortfolio();
        reqNames.add(portfolioBean.getName());
        if (CollectionUtils.isNotEmpty(portfolioBean.getSubPortfolios())) {
            portfolioBean.getSubPortfolios().forEach(subPortfolioBean -> {
                reqNames.add(subPortfolioBean.getName());
                addSubPortfolioNames(subPortfolioBean, reqNames);
            });
        }
        checkPortfolioNames(companyService.currentCompany(), reqNames);
    }

    private void addSubPortfolioNames(CreatePortfolioRequest.PortfolioBean portfolioBean, List<String> nameList) {
        if (CollectionUtils.isNotEmpty(portfolioBean.getSubPortfolios())) {
            portfolioBean.getSubPortfolios().forEach(subPortfolio -> {
                nameList.add(subPortfolio.getName());
                addSubPortfolioNames(subPortfolio, nameList);
            });
        }
    }

    private void checkPortfolioNames(Long companyId, List<String> names) {
        if (CollectionUtils.isNotEmpty(names)) {
            List<String> distinctNames = names.stream().distinct().collect(Collectors.toList());
            if (distinctNames.size() < names.size()) {
                throw new CodedException(HttpStatus.BAD_REQUEST, "项目集名称重复！");
            }
            PortfolioExample example = new PortfolioExample();
            example.createCriteria().andCompanyIdEqualTo(companyId).andNameIn(names);
            long count = portfolioMapper.countByExample(example);
            if (count > 0) {
                throw new CodedException(HttpStatus.BAD_REQUEST, "项目集名称重复！");
            }
        }
    }

    private void checkPortfolioMaxLayer(List<Portfolio> ancestor, CreatePortfolioRequest createPortfolioRequest) {
        int layer = 1;
        if (CollectionUtils.isNotEmpty(ancestor)) {
            layer += ancestor.size();
        }
        layer = getMaxLayer(createPortfolioRequest.getPortfolio(), layer);
        if (layer > maxPortfolioLayer) {
            throw new CodedException(HttpStatus.BAD_REQUEST, "超过项目集最大层级");
        }
    }

    private int getMaxLayer(CreatePortfolioRequest.PortfolioBean portfolioBean, int layer) {
        List<CreatePortfolioRequest.PortfolioBean> subPortfolios = portfolioBean.getSubPortfolios();
        if (CollectionUtils.isEmpty(subPortfolios)) {
            return layer;
        } else {
            int maxLayer = 0;
            layer++;
            for (CreatePortfolioRequest.PortfolioBean subPortfolio : subPortfolios) {
                int temp = getMaxLayer(subPortfolio, layer);
                if (temp > maxLayer) {
                    maxLayer = temp;
                }
            }
            return maxLayer;
        }
    }
}
