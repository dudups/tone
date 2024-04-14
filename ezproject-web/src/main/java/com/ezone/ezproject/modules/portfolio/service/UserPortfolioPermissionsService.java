package com.ezone.ezproject.modules.portfolio.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.dal.entity.Portfolio;
import com.ezone.ezproject.dal.entity.PortfolioExample;
import com.ezone.ezproject.dal.mapper.ExtPortfolioMapper;
import com.ezone.ezproject.es.entity.PortfolioRoleSchema;
import com.ezone.ezproject.es.entity.UserPortfolioPermissions;
import com.ezone.ezproject.es.entity.enums.PortfolioOperationType;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.redisson.RedissonMap;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class UserPortfolioPermissionsService {
    private UserService userService;

    private CompanyService companyService;

    private PortfolioQueryService portfolioQueryService;

    private PortfolioMemberQueryService portfolioMemberQueryService;

    private PortfolioSchemaQueryService portfolioSchemaQueryService;

    private ExtPortfolioMapper portfolioMapper;

    private RedissonSpringCacheManager cacheManager;

    public static final CodedException NOT_FOUND = new CodedException(HttpStatus.NOT_FOUND, "项目集不存在！");

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {
                    "cache:UserPortfolioPermissionsService.userPortfolioPermissions"
            },
            key = "'k:'.concat(#portfolioId).concat(':').concat(#user)",
            unless = "#result == null"
    )
    public UserPortfolioPermissions userPortfolioPermissions(String user, Long portfolioId) {
        Long company = portfolioQueryService.getPortfolioCompany(portfolioId);
        if (null == company) {
            throw NOT_FOUND;
        }
        if (userService.isCompanyAdmin(user, company)) {
            return UserPortfolioPermissions.fromAdmin(user);
        }
        List<String> roles = portfolioMemberQueryService.selectUserPortfolioRoles(company, user, portfolioId);
        if (CollectionUtils.isEmpty(roles)) {
            return null;
        }
        PortfolioRoleSchema schema = portfolioSchemaQueryService.getPortfolioRoleSchema(portfolioId);
        return UserPortfolioPermissions.from(user, schema.getRoles().stream().filter(role -> roles.contains(role.getKey())).collect(Collectors.toList()));
    }

    /**
     * 项目下：立刻清理指定user权限缓存，异步清理其它用户的权限缓存
     *
     * @param portfolioId
     * @param user
     */
    @AfterCommit
    @SneakyThrows
    public void cacheEvict(Long portfolioId, String user) {
        cacheEvict(portfolioId, user, false);
    }

    @AfterCommit
    @SneakyThrows
    public void cacheEvict(Long portfolioId, String user, boolean onlyEvictForUser) {
        if (StringUtils.isEmpty(user)) {
            if (onlyEvictForUser) {
                return;
            }
            cacheEvict(portfolioId);
            return;
        }
        Cache cache = cacheManager.getCache("cache:UserPortfolioPermissionsService.userPortfolioPermissions");
        String userCacheKey = String.format("k:%s:%s", portfolioId, user);
        cache.evict(userCacheKey);
        if (onlyEvictForUser) {
            return;
        }

        RedissonMap map = (RedissonMap) FieldUtils.readField(cache, "map", true);
        map.fastRemoveAsync(map
                .keySet(String.format("*k:%s:*", portfolioId), 100).stream()
                .filter(k -> !userCacheKey.equals(k)).toArray(String[]::new)
        );
    }

    /**
     * 异步清理项目下所有用户权限缓存
     *
     * @param portfolioId
     */
    @AfterCommit
    @SneakyThrows
    public void cacheEvict(Long portfolioId) {
        Cache cache = cacheManager.getCache("cache:UserPortfolioPermissionsService.userPortfolioPermissions");
        RedissonMap map = (RedissonMap) FieldUtils.readField(cache, "map", true);
        map.fastRemoveAsync(map.keySet(String.format("*k:%s:*", portfolioId), 100).stream().toArray(String[]::new));
    }

    public void checkHasPortfolioRead(Long portfolioId) throws CodedException {
        checkHasPortfolioRead(userService.currentUserName(), portfolioId);
    }

    protected void checkHasPortfolioRead(String user, Long projectId) throws CodedException {
        checkPermission(user, projectId, PortfolioOperationType.PORTFOLIO_READ);
    }

    public void checkPermission(String user, Long portfolioId, PortfolioOperationType op) {
        UserPortfolioPermissions permissions = permissions(user, portfolioId);
        if (permissions == null || !permissions.hasPermission(op)) {
            throw CodedException.FORBIDDEN;
        }
    }

    public void checkPermission(String user, Portfolio portfolio, PortfolioOperationType op) {
        UserPortfolioPermissions permissions = permissions(user, portfolio);
        if (permissions == null || !permissions.hasPermission(op)) {
            throw CodedException.FORBIDDEN;
        }
    }

    public void checkHasPortfolioManager(List<Long> portfolioIds) throws CodedException {
        if (CollectionUtils.isEmpty(portfolioIds)) {
            return;
        }
        PortfolioExample example = new PortfolioExample();
        example.createCriteria().andIdIn(portfolioIds);
        List<Portfolio> portfolios = portfolioMapper.selectByExample(example);
        if (CollectionUtils.size(portfolios) != portfolioIds.size()) {
            throw new CodedException(HttpStatus.NOT_FOUND, "项目集未找到");
        }
        portfolios.forEach(portfolio ->
                checkPermission(userService.currentUserName(), portfolio, PortfolioOperationType.PORTFOLIO_MANAGE_UPDATE)
        );
    }

    public void checkPermissionAnyOps(String user, Long portfolioId, PortfolioOperationType... ops) {
        UserPortfolioPermissions permissions = permissions(user, portfolioId);
        if (permissions == null) {
            throw CodedException.FORBIDDEN;
        }
        boolean hasAnyPermission = false;
        for (PortfolioOperationType op : ops) {
            if (permissions.hasPermission(op)) {
                hasAnyPermission = true;
                break;
            }
        }
        if (!hasAnyPermission) {
            throw CodedException.FORBIDDEN;
        }
    }

    private UserPortfolioPermissions permissions(String user, Long portfolioId) {
        Portfolio portfolio = portfolioMapper.selectByPrimaryKey(portfolioId);
        if (null == portfolio) {
            throw new CodedException(HttpStatus.NOT_FOUND, "项目集未找到");
        }
        return SpringBeanFactory.getBean(UserPortfolioPermissionsService.class).userPortfolioPermissions(user, portfolioId);
    }

    private UserPortfolioPermissions permissions(String user, @NotNull Portfolio portfolio) {
        return SpringBeanFactory.getBean(UserPortfolioPermissionsService.class).userPortfolioPermissions(user, portfolio.getId());
    }

    public UserPortfolioPermissions permissions(Long portfolioId) {
        Long companyId = companyService.currentCompany();
        Portfolio portfolio = portfolioQueryService.select(portfolioId);
        if (null == portfolio) {
            throw new CodedException(HttpStatus.NOT_FOUND, "项目集未找到");
        }
        if (companyId == null || !companyId.equals(portfolio.getCompanyId())) {
            throw CodedException.COMPANY_OUT_FORBIDDEN;
        }
        String user = userService.currentUserName();
        return SpringBeanFactory.getBean(UserPortfolioPermissionsService.class).userPortfolioPermissions(user, portfolioId);
    }
}