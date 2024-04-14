package com.ezone.ezproject.modules.chart.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.ProjectChart;
import com.ezone.ezproject.dal.entity.ProjectChartGroup;
import com.ezone.ezproject.dal.entity.ProjectChartGroupExample;
import com.ezone.ezproject.dal.mapper.ProjectChartGroupMapper;
import com.ezone.ezproject.dal.mapper.ProjectChartMapper;
import com.ezone.ezproject.es.dao.ChartDao;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.chart.bean.ChartGroupRequest;
import com.ezone.ezproject.modules.chart.bean.ChartRequest;
import com.ezone.ezproject.modules.chart.config.Chart;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ChartCmdService {
    private ProjectChartGroupMapper chartGroupMapper;
    private ProjectChartMapper chartMapper;
    private ChartDao chartDao;

    private ChartQueryService chartQueryService;
    private ProjectSchemaQueryService schemaQueryService;

    private UserService userService;

    private ProjectCardSchemaHelper projectCardSchemaHelper;

    private SqlSessionTemplate sqlSessionTemplate;

    public ProjectChartGroup createGroup(Long projectId, ChartGroupRequest request) {
        String user = userService.currentUserName();
        ProjectChartGroup group = ProjectChartGroup.builder()
                .id(IdUtil.generateId())
                .projectId(projectId)
                .type(request.getType().name())
                .title(request.getTitle())
                .createTime(new Date())
                .createUser(user)
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .build();
        chartGroupMapper.insert(group);
        return group;
    }

    public ProjectChartGroup updateGroup(Long id, ChartGroupRequest request) {
        String user = userService.currentUserName();
        ProjectChartGroup group = chartGroupMapper.selectByPrimaryKey(id);
        group.setTitle(request.getTitle());
        group.setLastModifyTime(new Date());
        group.setLastModifyUser(user);
        chartGroupMapper.updateByPrimaryKey(group);
        return group;
    }

    public void deleteGroup(Long id) throws IOException {
        chartGroupMapper.deleteByPrimaryKey(id);
        List<ProjectChart> charts = chartQueryService.selectChartByGroupId(id);
        if (CollectionUtils.isEmpty(charts)) {
            return;
        }
        List<Long> chartIds = new ArrayList<>();
        charts.forEach(chart -> {
            chartMapper.deleteByPrimaryKey(chart.getId());
            chartIds.add(chart.getId());
        });
        chartDao.delete(chartIds);
    }

    public ProjectChart createChart(ProjectChartGroup group, ChartRequest request) throws IOException {
        String user = userService.currentUserName();
        ProjectCardSchema schema = schemaQueryService.getProjectCardSchema(group.getProjectId());
        Map<String, Long> fieldKeyIds = projectCardSchemaHelper.extractCustomFieldId(schema, request.getQueries());
        ProjectChart chart = ProjectChart.builder()
                .id(IdUtil.generateId())
                .projectId(group.getProjectId())
                .groupId(group.getId())
                .type(Chart.chartType(request.getConfig()))
                .title(request.getTitle())
                .seqNum(chartQueryService.nextChartSeqNum(group.getId()))
                .createTime(new Date())
                .createUser(user)
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .build();
        chartMapper.insert(chart);
        chartDao.saveOrUpdate(chart.getId(), Chart.builder()
                .queries(request.getQueries())
                .config(request.getConfig())
                .excludeNoPlan(request.isExcludeNoPlan())
                .fieldIds(fieldKeyIds)
                .build());

        return chart;
    }

    public ProjectChart updateChart(Long id, ChartRequest request) throws IOException {
        String user = userService.currentUserName();
        ProjectChart chart = chartMapper.selectByPrimaryKey(id);
        ProjectCardSchema schema = schemaQueryService.getProjectCardSchema(chart.getProjectId());
        Map<String, Long> fieldKeyIds = projectCardSchemaHelper.extractCustomFieldId(schema, request.getQueries());
        chart.setType(Chart.chartType(request.getConfig()));
        chart.setTitle(request.getTitle());
        chart.setLastModifyTime(new Date());
        chart.setLastModifyUser(user);
        chartMapper.updateByPrimaryKey(chart);
        chartDao.saveOrUpdate(chart.getId(), Chart.builder()
                .queries(request.getQueries())
                .config(request.getConfig())
                .excludeNoPlan(request.isExcludeNoPlan())
                .fieldIds(fieldKeyIds)
                .build());
        return chart;
    }

    public ProjectChart moveChart(ProjectChart chart, Long afterId) {
        if (chart.getId().equals(afterId)) {
            return chart;
        }
        List<ProjectChart> charts = chartQueryService.selectByProjectId(chart.getProjectId());
        List<ProjectChart> sortCharts = new ArrayList<>();
        if (afterId == null || afterId <= 0) {
            sortCharts.add(chart);
            sortCharts.addAll(sortCharts.stream().filter(c -> !c.getId().equals(chart.getId())).collect(Collectors.toList()));
        } else {
            boolean findAfter = false;
            for (int i = 0; i < charts.size(); i++) {
                ProjectChart item = charts.get(i);
                if (item.getId().equals(chart.getId())) {
                    continue;
                }
                sortCharts.add(item);
                if (!findAfter && item.getId().equals(afterId)) {
                    findAfter = true;
                    sortCharts.add(chart);
                }
            }
            if (!findAfter) {
                sortCharts.add(chart);
            }
        }
        for (int i = 0; i < sortCharts.size(); i++) {
            ProjectChart item = sortCharts.get(i);
            item.setSeqNum(i);
            chartMapper.updateByPrimaryKey(item);
        }
        return chart;
    }

    public void deleteChart(Long id) throws IOException {
        chartMapper.deleteByPrimaryKey(id);
        chartDao.delete(id);
    }

    public void deleteByProject(Long projectId) throws IOException {
        ProjectChartGroupExample example = new ProjectChartGroupExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        chartGroupMapper.deleteByExample(example);

        List<ProjectChart> charts = chartQueryService.selectByProjectId(projectId);
        if (CollectionUtils.isEmpty(charts)) {
            return;
        }
        List<Long> chartIds = new ArrayList<>();
        charts.forEach(chart -> {
            chartMapper.deleteByPrimaryKey(chart.getId());
            chartIds.add(chart.getId());
        });
        chartDao.delete(chartIds);
    }

    public void copyProjectChart(Long sourceProjectId, Long targetProjectId) throws IOException {
        String userName = userService.currentUserName();
        Map<Long, Long> sourceGroupKeys = copyProjectChartGroupByProject(sourceProjectId, targetProjectId, userName);
        List<ProjectChart> sourceProjectCharts = chartQueryService.selectByProjectId(sourceProjectId);
        if (CollectionUtils.isNotEmpty(sourceProjectCharts)) {
            List<Long> sourceChartIds = sourceProjectCharts.stream().map(ProjectChart::getId).collect(Collectors.toList());
            List<ProjectChart> targetProjectCharts = copyChartsForDb(targetProjectId, userName, sourceGroupKeys, sourceProjectCharts);
            List<Long> targetChartIds = targetProjectCharts.stream().map(ProjectChart::getId).collect(Collectors.toList());
            copyChartsForEs(sourceChartIds, targetChartIds);
        }
    }

    /**
     * 复制到数据库并返回新的projectChart
     *
     * @param targetProjectId     新的报表所属项目
     * @param userName            操作用户
     * @param groupIds            源报表组与目标报表组映射关系
     * @param sourceProjectCharts 源报表
     * @return 新的项目报表
     */
    private List<ProjectChart> copyChartsForDb(Long targetProjectId, String userName, Map<Long, Long> groupIds, List<ProjectChart> sourceProjectCharts) {
        List<ProjectChart> targetProjectCharts = new ArrayList<>();
        Date now = new Date();
        for (ProjectChart projectChart : sourceProjectCharts) {
            Long charId = IdUtil.generateId();
            Long groupId = projectChart.getGroupId();
            if (groupId == null) {
                continue;
            }
            ProjectChart chart = ProjectChart.builder()
                    .id(charId)
                    .title(projectChart.getTitle())
                    .projectId(targetProjectId)
                    .groupId(groupIds.get(groupId))
                    .createUser(userName)
                    .createTime(now)
                    .lastModifyUser(userName)
                    .lastModifyTime(now)
                    .type(projectChart.getType())
                    .seqNum(projectChart.getSeqNum())
                    .build();
            chartMapper.insert(chart);
            targetProjectCharts.add(chart);
        }
        return targetProjectCharts;
    }

    private void copyChartsForEs(List<Long> chartIds, List<Long> newChartIds) throws IOException {
        List<Chart> charts = chartQueryService.selectChartByIds(chartIds);
        Map<Long, Chart> chartMap = new HashMap<>();
        for (int i = 0; i < chartIds.size(); i++) {
            chartMap.put(newChartIds.get(i), charts.get(i));
        }
        chartDao.saveOrUpdate(chartMap);
    }

    /**
     * 拷贝项目报表组目录
     *
     * @param sourceProjectId 源项目ID
     * @param targetProjectId 目法项目ID
     * @param userName        操作用户名
     * @return 返回map中key为旧的报表组ID, value为对应新的报表组ID。
     */
    private Map<Long, Long> copyProjectChartGroupByProject(Long sourceProjectId, Long targetProjectId, String userName) {
        List<ProjectChartGroup> projectChartGroups = chartQueryService.selectGroupByProjectId(sourceProjectId);
        Map<Long, Long> oldNewGroupKeys = new HashMap<>();
        if (CollectionUtils.isNotEmpty(projectChartGroups)) {
            Date now = new Date();
            for (ProjectChartGroup projectChartGroup : projectChartGroups) {
                Long groupId = IdUtil.generateId();
                oldNewGroupKeys.put(projectChartGroup.getId(), groupId);
                chartGroupMapper.insert(ProjectChartGroup.builder()
                        .id(groupId)
                        .title(projectChartGroup.getTitle())
                        .type(projectChartGroup.getType())
                        .projectId(targetProjectId)
                        .createTime(now)
                        .createUser(userName)
                        .lastModifyTime(now)
                        .lastModifyUser(userName)
                        .build());
            }
        }
        return oldNewGroupKeys;
    }

    /**
     * 复制报表组及组中的报表
     *
     * @param sourceGroup 原报表组
     * @param request     新建报表请求数据
     * @return
     */
    public ProjectChartGroup copyGroupAndReport(ProjectChartGroup sourceGroup, ChartGroupRequest request) throws IOException {
        final String user = userService.currentUserName();
        ProjectChartGroup targetGroup = ProjectChartGroup.builder()
                .id(IdUtil.generateId())
                .projectId(sourceGroup.getProjectId())
                .type(request.getType().name())
                .title(request.getTitle())
                .createTime(new Date())
                .createUser(user)
                .lastModifyTime(new Date())
                .lastModifyUser(user)
                .build();
        chartGroupMapper.insert(targetGroup);
        List<ProjectChart> sourceProjectCharts = chartQueryService.selectChartByGroupId(sourceGroup.getId());
        if (CollectionUtils.isNotEmpty(sourceProjectCharts)) {
            List<Long> sourceChartIds = sourceProjectCharts.stream().map(ProjectChart::getId).collect(Collectors.toList());
            List<Long> targetChartIds = new ArrayList<>();
            sourceProjectCharts.forEach(sourceProjectChart -> {
                ProjectChart targetChart = ProjectChart.builder()
                        .id(IdUtil.generateId())
                        .projectId(sourceProjectChart.getProjectId())
                        .groupId(targetGroup.getId())
                        .type(sourceProjectChart.getType())
                        .title(sourceProjectChart.getTitle())
                        .seqNum(sourceProjectChart.getSeqNum())
                        .createTime(new Date())
                        .createUser(user)
                        .lastModifyTime(new Date())
                        .lastModifyUser(user)
                        .build();
                chartMapper.insert(targetChart);
                targetChartIds.add(targetChart.getId());
            });
            copyChartsForEs(sourceChartIds, targetChartIds);
        }
        return targetGroup;
    }
}
