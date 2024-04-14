/**
 * 20220830
 * 项目角色支持拖动排序，此脚本用于给原企业角色加上排序值，角色排序是按照rank值从小到大排序。
 */


import com.ezone.ezproject.es.dao.ProjectRoleSchemaDao
import com.ezone.ezproject.es.entity.ProjectRoleSchema
import com.ezone.ezproject.es.entity.enums.RoleSource
import com.ezone.ezproject.modules.company.service.RoleRankCmdService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def roleRankCmdService = SpringBeanFactory.getBean(RoleRankCmdService.class)
def projectRoleSchemaDao = SpringBeanFactory.getBean(ProjectRoleSchemaDao.class)

def setCustomerRoleRank = {
    def pageNum = 1
    def pageSize = 200
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.forEach { project ->
            println("执行更新")
            ProjectRoleSchema roleSchema = projectRoleSchemaDao.find(project.getId());
            if (roleSchema) {
                roleSchema.setMaxRank(RoleRankCmdService.COMPANY_FIRST_ROLE_DEFAULT_RANK)
                roleSchema.getRoles().forEach { role ->
                    if (role.getSource() == RoleSource.CUSTOM) {
                        role.setRank(roleRankCmdService.nextRank(roleSchema))
                    }
                }
                projectRoleSchemaDao.saveOrUpdate(project.getId(), roleSchema)
            }
        }
        pageNum++
    }
}

try {
    setCustomerRoleRank()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"