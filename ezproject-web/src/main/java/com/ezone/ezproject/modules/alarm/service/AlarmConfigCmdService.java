package com.ezone.ezproject.modules.alarm.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.exception.ErrorCode;
import com.ezone.ezproject.dal.entity.ProjectAlarm;
import com.ezone.ezproject.dal.entity.ProjectAlarmExample;
import com.ezone.ezproject.dal.mapper.ProjectAlarmMapper;
import com.ezone.ezproject.es.dao.ProjectAlarmDao;
import com.ezone.ezproject.es.entity.ProjectAlarmExt;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.alarm.bean.AlarmItem;
import com.ezone.ezproject.modules.alarm.bean.PlanAlarmItem;
import com.ezone.ezproject.modules.alarm.bean.ProjectAlarmItem;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class AlarmConfigCmdService {
    private ProjectAlarmDao projectAlarmDao;
    private AlarmNoticeService alarmNoticeService;
    private ProjectAlarmMapper projectAlarmMapper;
    private UserService userService;

    public ProjectAlarmExt add(Long projectId, AlarmItem alarmItem, @NotNull Boolean active) throws IOException {
        if (alarmItem == null) {
            return null;
        }
        checkName(projectId, alarmItem.getName(), null);
        Long id = IdUtil.generateId();
        String user = userService.currentUserName();
        Date now = new Date();
        ProjectAlarmExt projectAlarmExt = ProjectAlarmExt.builder()
                .id(id)
                .projectId(projectId)
                .alarmItem(alarmItem)
                .active(active)
                .createTime(now)
                .createUser(user)
                .lastModifyTime(now)
                .type(getAlarmType(alarmItem).name())
                .lastModifyUser(user)
                .build();
        projectAlarmMapper.insert(formProjectAlarmExt(projectAlarmExt));
        projectAlarmDao.saveOrUpdate(projectAlarmExt.getId(), projectAlarmExt);
        alarmNoticeService.genNoticePlans(projectAlarmExt, null, null);
        return projectAlarmExt;
    }

    public AlarmItem.Type getAlarmType(AlarmItem alarmItem) {
        if (alarmItem instanceof ProjectAlarmItem) {
            return AlarmItem.Type.projectAlarm;
        } else if (alarmItem instanceof PlanAlarmItem) {
            return AlarmItem.Type.planAlarm;
        } else {
            return AlarmItem.Type.cardAlarm;
        }
    }


    public void copy(Long projectId, List<ProjectAlarmExt> projectAlarms) throws IOException {
        if (CollectionUtils.isEmpty(projectAlarms)) {
            return;
        }
        List<ProjectAlarmExt> copyProjectAlarms = projectAlarms.stream()
                .map(projectAlarm -> ProjectAlarmExt.builder()
                        .id(IdUtil.generateId())
                        .projectId(projectId)
                        .alarmItem(projectAlarm.getAlarmItem())
                        .type(projectAlarm.getType())
                        .active(projectAlarm.getActive()).build())
                .collect(Collectors.toList());
        projectAlarmDao.saveOrUpdate(copyProjectAlarms);
    }

    public ProjectAlarmExt update(Long projectId, Long projectAlarmId, AlarmItem alarmItem) throws IOException {
        if (alarmItem == null) {
            return null;
        }
        checkName(projectId, alarmItem.getName(), projectAlarmId);
        ProjectAlarmExt oldProjectAlarm = projectAlarmDao.find(projectAlarmId);
        ProjectAlarmExt projectAlarm = ProjectAlarmExt.builder()
                .id(projectAlarmId)
                .createUser(oldProjectAlarm.getCreateUser())
                .createTime(oldProjectAlarm.getCreateTime())
                .lastModifyUser(userService.currentUserName())
                .lastModifyTime(new Date())
                .projectId(projectId)
                .alarmItem(alarmItem)
                .type(getAlarmType(alarmItem).name())
                .active(oldProjectAlarm.getActive()).build();
        projectAlarmMapper.updateByPrimaryKey(formProjectAlarmExt(projectAlarm));
        projectAlarmDao.saveOrUpdate(projectAlarmId, projectAlarm);
        AlarmItem oldAlarmItem = oldProjectAlarm.getAlarmItem();
        if (!oldAlarmItem.getDateFieldKey().equals(alarmItem.getDateFieldKey()) || !oldAlarmItem.getAlarmDateConfig().equals(alarmItem.getAlarmDateConfig())) {
            alarmNoticeService.updateNoticePlans(projectAlarm, null, null);
        }
        return projectAlarm;
    }

    public void delete(Long projectAlarmId) throws IOException {
        projectAlarmMapper.deleteByPrimaryKey(projectAlarmId);
        ProjectAlarmExt oldProjectAlarm = projectAlarmDao.find(projectAlarmId);
        projectAlarmDao.delete(projectAlarmId);
        alarmNoticeService.asyncDeleteNoticePlans(oldProjectAlarm);

    }

    public void active(Long projectAlarmId, Boolean active) throws IOException {
        ProjectAlarmExt projectAlarm = projectAlarmDao.find(projectAlarmId);
        projectAlarm.setActive(active);
        projectAlarmDao.saveOrUpdate(projectAlarmId, projectAlarm);
    }

    private ProjectAlarm formProjectAlarmExt(ProjectAlarmExt projectAlarmExt) {
        return ProjectAlarm.builder()
                .id(projectAlarmExt.getId())
                .projectId(projectAlarmExt.getProjectId())
                .name(projectAlarmExt.getAlarmItem().getName())
                .build();
    }

    private void checkName(Long projectId, String name, Long excludeId) {
        ProjectAlarmExample example = new ProjectAlarmExample();
        ProjectAlarmExample.Criteria criteria = example.createCriteria().andProjectIdEqualTo(projectId).andNameEqualTo(name);
        if (excludeId != null && excludeId > 0) {
            criteria.andIdNotEqualTo(excludeId);
        }
        if (projectAlarmMapper.countByExample(example) > 0) {
            throw new CodedException(ErrorCode.KEY_CONFLICT, "名称冲突!");
        }
    }
}