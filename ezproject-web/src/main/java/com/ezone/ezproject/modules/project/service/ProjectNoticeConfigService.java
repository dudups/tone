package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.configuration.CacheManagers;
import com.ezone.ezproject.es.dao.ProjectNoticeConfigDao;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.bean.ProjectConfigRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectNoticeConfigService {
    private ProjectNoticeConfigDao projectNoticeConfigDao;

    private UserService userService;

    @CacheEvict(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectNoticeConfigService.getProjectNoticeConfig"},
            key = "#projectId"
    )
    public void saveOrUpdate(Long projectId, ProjectConfigRequest request) throws IOException {
        String user = userService.currentUserName();
        ProjectNoticeConfig board = ProjectNoticeConfig.builder()
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .planNoticeConfig(request.getPlanNoticeConfig())
                .cardNoticeConfig(request.getCardNoticeConfig())
                .build();
        projectNoticeConfigDao.saveOrUpdate(projectId, board);
    }

    @Cacheable(
            cacheManager = CacheManagers.REDISSON_CACHE_MANAGER,
            cacheNames = {"cache:ProjectNoticeConfigService.getProjectNoticeConfig"},
            key = "#projectId",
            unless = "#result == null"
    )
    public ProjectNoticeConfig getProjectNoticeConfig(Long projectId) throws IOException {
        ProjectNoticeConfig config = projectNoticeConfigDao.find(projectId);
        if (config == null) {
            config = ProjectNoticeConfig.DEFAULT_CONFIG;
        }
        return config;
    }


    public void initProjectNoticeConfig(Long projectId, ProjectNoticeConfig projectNoticeConfig) throws IOException {
        if (projectNoticeConfig == null) {
            projectNoticeConfig = ProjectNoticeConfig.DEFAULT_CONFIG;
        }
        projectNoticeConfigDao.saveOrUpdate(projectId, projectNoticeConfig);
    }
}
