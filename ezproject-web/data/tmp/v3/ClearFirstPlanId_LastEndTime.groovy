/**
 * 20220519:
 * ezone dsw大版本内解决，只需要刷在线版服务
 * 字段first_plan_id的limit：build_in未设置，字段last_end_time的name冲突；把保存了错误配置的schema刷新下
 */

import com.ezone.ezproject.es.dao.ProjectCardSchemaDao
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)

def resetSchema = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.forEach {
            def projectId = it.id
            def schema = schemaDao.find(projectId)
            if (schema && schema.fields) {
                schema.fields = schema.fields.findAll {it.key != 'first_plan_id' && it.key != 'last_end_time'}
                schemaDao.saveOrUpdate(projectId, schema)
            }
        }
        pageNum++
    }
}

try {
    resetSchema()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"