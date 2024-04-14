package com.ezone.ezproject.modules.chart.service;

import com.ezone.ezproject.dal.entity.ProjectChart;
import com.ezone.ezproject.dal.entity.ProjectChartExample;
import com.ezone.ezproject.dal.entity.ProjectChartGroup;
import com.ezone.ezproject.dal.entity.ProjectChartGroupExample;
import com.ezone.ezproject.dal.mapper.ProjectChartGroupMapper;
import com.ezone.ezproject.dal.mapper.ProjectChartMapper;
import com.ezone.ezproject.es.dao.ChartDao;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.chart.config.Chart;
import com.ezone.ezproject.modules.chart.enums.ChartGroupType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class ChartQueryService {
    private ProjectChartGroupMapper chartGroupMapper;
    private ProjectChartMapper chartMapper;
    private ChartDao chartDao;

    private UserService userService;

    public List<ProjectChartGroup> selectGroupByProjectId(Long projectId) {
        ProjectChartGroupExample example = new ProjectChartGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return chartGroupMapper.selectByExample(example);
    }

    public List<ProjectChartGroup> selectGroupForUser(Long projectId) {
        String user = userService.currentUserName();
        ProjectChartGroupExample example = new ProjectChartGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andTypeEqualTo(ChartGroupType.SHARE.name());
        example.or().andProjectIdEqualTo(projectId).andTypeEqualTo(ChartGroupType.USER.name()).andCreateUserEqualTo(user);
        return chartGroupMapper.selectByExample(example);
    }

    public ProjectChartGroup selectChartGroup(Long id) {
        return chartGroupMapper.selectByPrimaryKey(id);
    }

    public List<ProjectChart> selectChartByGroupId(Long groupId) {
        ProjectChartExample example = new ProjectChartExample();
        example.createCriteria().andGroupIdEqualTo(groupId);
        return chartMapper.selectByExample(example);
    }

    public int nextChartSeqNum(Long groupId) {
        ProjectChartExample example = new ProjectChartExample();
        example.createCriteria().andGroupIdEqualTo(groupId);
        return (int) chartMapper.countByExample(example);
    }

    public List<ProjectChart> selectByProjectId(Long projectId) {
        ProjectChartExample example = new ProjectChartExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return chartMapper.selectByExample(example);
    }

    public Chart selectChart(Long id) throws IOException {
        return chartDao.find(id);
    }

    public List<Chart> selectChartByIds(List<Long> ids) throws IOException {
        return chartDao.find(ids);
    }

    public ProjectChart selectProjectChart(Long id) throws IOException {
        return chartMapper.selectByPrimaryKey(id);
    }
}
