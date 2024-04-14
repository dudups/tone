package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.es.dao.ProjectMenuDao;
import com.ezone.ezproject.es.entity.ProjectMenuConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.project.bean.ProjectMenu;
import com.ezone.ezproject.modules.project.bean.ProjectMenuConfigReq;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectMenuService {
    private ProjectMenuDao projectMenuDao;
    private UserService userServer;

    public ProjectMenuConfig getDefaultMenuConfig() {
        return ProjectMenuConfig.builder().menus(Arrays.asList(ProjectMenu.values())).defaultMenu(ProjectMenu.plan).build();
    }

    public ProjectMenuConfig getMenuConfig(Long projectId) {
        ProjectMenuConfig projectMenu = null;
        try {
            projectMenu = projectMenuDao.find(projectId);
        } catch (IOException e) {
            log.error("[getOpenMenus][" + " projectId :" + projectId + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (projectMenu == null || CollectionUtils.isEmpty(projectMenu.getMenus())) {
            return getDefaultMenuConfig();
        }
        return projectMenu;
    }

    public void saveOrUpdateOpenMenus(Long projectId, ProjectMenuConfigReq projectMenuReq) {
        ProjectMenuConfig projectMenu;
        try {
            projectMenu = projectMenuDao.find(projectId);
        } catch (IOException e) {
            log.error("[getOpenMenus][" + " projectId :" + projectId + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (projectMenu == null) {
            projectMenu = ProjectMenuConfig.builder().build();
        }
        projectMenu.setLastModifyUser(userServer.currentUserName());
        projectMenu.setLastModifyTime(new Date());
        projectMenu.setMenus(projectMenuReq.getMenus());
        projectMenu.setDefaultMenu(projectMenuReq.getDefaultMenu());
        try {
            projectMenuDao.saveOrUpdate(projectId, projectMenu);
        } catch (IOException e) {
            log.error("[updateOpenMenus][" + " projectId :" + projectId + "; openMenus :" + projectMenuReq.getMenus() + "][error][" + e.getMessage() + "]", e);
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    public ProjectMenuConfig find(Long projectId) throws IOException {
        return projectMenuDao.find(projectId);
    }

    public void delete(Long projectId) throws IOException {
        projectMenuDao.delete(projectId);
    }

    public void delete(List<Long> projectIds) throws IOException {
        projectMenuDao.delete(projectIds);
    }
}
