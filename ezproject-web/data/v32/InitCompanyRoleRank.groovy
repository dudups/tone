/**
 * 20220816
 * 企业角色支持拖动排序，此脚本用于给原企业角色加上排序值，角色排序是按照rank值从小到大排序。
 */


import com.ezone.ezproject.es.dao.CompanyProjectRoleSchemaDao
import com.ezone.ezproject.es.entity.enums.RoleSource
import com.ezone.ezproject.es.util.EsIndexUtil
import com.ezone.ezproject.modules.company.service.RoleRankCmdService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def companyProjectRoleSchemaDao = SpringBeanFactory.getBean(CompanyProjectRoleSchemaDao.class)
def companyRoleRankCmdService = SpringBeanFactory.getBean(RoleRankCmdService.class)
def setCompanyRoleRank = {
    def companyRoleSchemaIds = companyProjectRoleSchemaDao.searchAllIds(EsIndexUtil.indexForCompanyProjectRoleSchema(), null)
    companyRoleSchemaIds.forEach(
            {id ->
                def roleSchema = companyProjectRoleSchemaDao.find(id)
                roleSchema.setMaxRank(RoleRankCmdService.COMPANY_FIRST_ROLE_DEFAULT_RANK)
                def roles =  roleSchema.getRoles()
                for (i in 0..roles.size()-1) {
                    if ( roles.get(i).getSource() == RoleSource.COMPANY) {
                        roles.get(i).setRank(companyRoleRankCmdService.nextRank(roleSchema))
                    } else {
                        roles.get(i).setRank(null)
                    }
                }
                companyProjectRoleSchemaDao.saveOrUpdate(id, roleSchema)
            }
    )
}

try {
    setCompanyRoleRank()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"