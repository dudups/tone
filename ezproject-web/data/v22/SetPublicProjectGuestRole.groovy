/**
 * 20220424:
 * 项目自定义角色，刷历史数据：历史公开项目添加all为GUEST角色
 */


import com.ezone.ezbase.iam.bean.enums.GroupUserType
import com.ezone.ezproject.common.IdUtil
import com.ezone.ezproject.dal.entity.ProjectExample
import com.ezone.ezproject.dal.entity.ProjectMember
import com.ezone.ezproject.dal.entity.ProjectMemberExample
import com.ezone.ezproject.dal.mapper.ProjectMapper
import com.ezone.ezproject.dal.mapper.ProjectMemberMapper
import com.ezone.ezproject.es.entity.ProjectRole
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import com.github.pagehelper.PageHelper
import org.apache.commons.lang.exception.ExceptionUtils

def projectMapper = SpringBeanFactory.getBean(ProjectMapper.class)
def projectMemberMapper = SpringBeanFactory.getBean(ProjectMemberMapper.class)

def filterGuestAllProjectIds = {List<Long> projectIds ->
    ProjectMemberExample example = new ProjectMemberExample()
    example.createCriteria().andProjectIdIn(projectIds).andUserTypeEqualTo('GROUP').andUserEqualTo('all')
    return projectMemberMapper.selectByExample(example).collect {it.id}
}

def setPublicProjectGuestRole = {
    def pageNum = 1
    def pageSize = 1000
    ProjectExample example = new ProjectExample()
    example.createCriteria().andIsPrivateEqualTo(false)
    while (true) {
        PageHelper.startPage(pageNum, pageSize, true)
        def projects = projectMapper.selectByExample(example)
        if (!projects) {
            break
        }
        def guestAllProjectIds = filterGuestAllProjectIds(projects.collect {it.id})
        projects.findAll {!guestAllProjectIds.contains(it.id)}.forEach {
            projectMemberMapper.insert(ProjectMember.builder()
                    .id(IdUtil.generateId())
                    .projectId(it.id)
                    .userType(GroupUserType.GROUP.name())
                    .user('all')
                    .companyId(it.companyId)
                    .role(ProjectRole.GUEST)
                    .build())
        }
        pageNum++
    }
}

try {
    setPublicProjectGuestRole()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"