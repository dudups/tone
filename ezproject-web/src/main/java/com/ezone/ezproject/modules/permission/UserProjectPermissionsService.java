package com.ezone.ezproject.modules.permission;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.entity.ProjectRoleSchema;
import com.ezone.ezproject.es.entity.UserProjectPermissions;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.service.ProjectMemberQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class UserProjectPermissionsService {
    private UserService userService;

    private ProjectQueryService projectQueryService;

    private ProjectMemberQueryService projectMemberQueryService;

    private ProjectSchemaQueryService projectSchemaQueryService;

    private RedissonSpringCacheManager cacheManager;

    public static final CodedException NOT_FOUND = new CodedException(HttpStatus.NOT_FOUND, "项目不存在！");

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {
                    "cache:UserProjectPermissionsService.userProjectPermissions"
            },
            key = "'k:'.concat(#projectId).concat(':').concat(#user)",
            unless = "#result == null"
    )
    public UserProjectPermissions userProjectPermissions(String user, Long projectId) {
        Long company = projectQueryService.getProjectCompany(projectId);
        if (null == company) {
            throw NOT_FOUND;
        }
        if (userService.isCompanyAdmin(user, company)) {
            return UserProjectPermissions.fromAdmin(user);
        }
        List<String> roles = projectMemberQueryService.selectUserProjectRoles(company, user, projectId);
        if (CollectionUtils.isEmpty(roles)) {
            return null;
        }
        ProjectRoleSchema schema = projectSchemaQueryService.getProjectRoleSchema(projectId);
        return UserProjectPermissions.from(user, schema.getRoles().stream().filter(role -> roles.contains(role.getKey())).collect(Collectors.toList()));
    }

    /**
     * 项目下：立刻清理指定user权限缓存，异步清理其它用户的权限缓存
     * @param projectId
     * @param user
     */
    @AfterCommit
    @SneakyThrows
    public void cacheEvict(Long projectId, String user) {
        cacheEvict(projectId, user, false);
    }

    @AfterCommit
    @SneakyThrows
    public void cacheEvict(Long projectId, String user, boolean onlyEvictForUser) {
        if (StringUtils.isEmpty(user)) {
            if (onlyEvictForUser) {
                return;
            }
            cacheEvict(projectId);
            return;
        }
        Cache cache = cacheManager.getCache("cache:UserProjectPermissionsService.userProjectPermissions");
        String userCacheKey = String.format("k:%s:%s", projectId, user);
        cache.evict(userCacheKey);
        if (onlyEvictForUser) {
            return;
        }

        RedissonMap map = (RedissonMap) FieldUtils.readField(cache, "map", true);
        map.fastRemoveAsync(map
                .keySet(String.format("*k:%s:*", projectId), 100).stream()
                .filter(k -> !userCacheKey.equals(k)).toArray(String[]::new)
        );
    }

    /**
     * 异步清理项目下所有用户权限缓存
     * @param projectId
     */
    @AfterCommit
    @SneakyThrows
    public void cacheEvict(Long projectId) {
        Cache cache = cacheManager.getCache("cache:UserProjectPermissionsService.userProjectPermissions");
        RedissonMap map = (RedissonMap) FieldUtils.readField(cache, "map", true);
        map.fastRemoveAsync(map.keySet(String.format("*k:%s:*", projectId), 100).stream().toArray(String[]::new));
    }

}
