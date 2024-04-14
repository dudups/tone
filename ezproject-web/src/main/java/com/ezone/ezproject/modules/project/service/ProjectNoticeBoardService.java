package com.ezone.ezproject.modules.project.service;

import com.ezone.ezproject.es.dao.ProjectNoticeBoardDao;
import com.ezone.ezproject.es.entity.ProjectNoticeBoard;
import com.ezone.ezproject.ez.context.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ProjectNoticeBoardService {
    private ProjectNoticeBoardDao projectNoticeBoardDao;
    
    private UserService userService;

    public void saveOrUpdate(Long projectId, String content) throws IOException {
        String user = userService.currentUserName();
        ProjectNoticeBoard board = ProjectNoticeBoard.builder()
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .content(content)
                .build();
        projectNoticeBoardDao.saveOrUpdate(projectId, board);
    }

    public ProjectNoticeBoard find(Long projectId) throws IOException {
        return projectNoticeBoardDao.find(projectId);
    }

    public void delete(Long projectId) throws IOException {
        projectNoticeBoardDao.delete(projectId);
    }

    public void delete(List<Long> projectIds) throws IOException {
        projectNoticeBoardDao.delete(projectIds);
    }

}
