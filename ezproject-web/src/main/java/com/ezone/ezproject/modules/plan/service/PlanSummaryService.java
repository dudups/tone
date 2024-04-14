package com.ezone.ezproject.modules.plan.service;

import com.ezone.ezproject.es.dao.PlanSummaryDao;
import com.ezone.ezproject.es.entity.PlanSummary;
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
public class PlanSummaryService {
    private PlanSummaryDao planSummaryDao;
    
    private UserService userService;

    public void saveOrUpdate(Long planId, String content) throws IOException {
        String user = userService.currentUserName();
        PlanSummary summary = PlanSummary.builder()
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .content(content)
                .build();
        planSummaryDao.saveOrUpdate(planId, summary);
    }

    public PlanSummary find(Long planId) throws IOException {
        return planSummaryDao.find(planId);
    }

    public void delete(Long planId) throws IOException {
        planSummaryDao.delete(planId);
    }

    public void delete(List<Long> planIds) throws IOException {
        planSummaryDao.delete(planIds);
    }

}
