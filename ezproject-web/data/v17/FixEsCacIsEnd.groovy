/**
 * 20220324:
 * 项目下因更改状态是否是结束状态定义时，更新条件漏了卡片类型，导致不同类型的结束状态定义不一样的情况下，calc_is_end字段值错误
 */

import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.card.bean.query.In
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaQueryService = SpringBeanFactory.getBean(ProjectSchemaQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)

def fixCalcIsEnd = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.forEach {
            def projectId = it.id
            def schema = schemaQueryService.getProjectCardSchema(projectId)
            schema && schema.getTypes().forEach {
                def endStatuses = it.findStatusKeys(true)
                if (endStatuses) {
                    def queries = [
                            Eq.builder().field(CardField.PROJECT_ID).value("$projectId").build(),
                            Eq.builder().field(CardField.TYPE).value(it.key).build(),
                            Eq.builder().field(CardField.CALC_IS_END).value("false").build(),
                            In.builder().field(CardField.STATUS).values(endStatuses).build()
                    ]
                    cardDao.updateByQuery(queries, CardField.CALC_IS_END, true)
                }
                def notEndStatuses = it.findStatusKeys(false)
                if (notEndStatuses) {
                    def queries = [
                            Eq.builder().field(CardField.PROJECT_ID).value("$projectId").build(),
                            Eq.builder().field(CardField.TYPE).value(it.key).build(),
                            Eq.builder().field(CardField.CALC_IS_END).value("true").build(),
                            In.builder().field(CardField.STATUS).values(notEndStatuses).build()
                    ]
                    cardDao.updateByQuery(queries, CardField.CALC_IS_END, false)
                }
            }
        }
        pageNum++
    }
}

try {
    fixCalcIsEnd()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"