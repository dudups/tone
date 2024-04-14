/**
 * 20210518:
 * 根据schema定义和卡片type+status计算是否完成的冗余信息；
 */

import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.card.bean.query.In
import com.ezone.ezproject.modules.card.bean.query.NotEq
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaQueryService = SpringBeanFactory.getBean(ProjectSchemaQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)

def init = {
    def query = NotEq.builder().field(CardField.CALC_IS_END).value("true").build()
    cardDao.updateByQuery(query, CardField.CALC_IS_END, false)
    cardEventDao.setCalcIsEnd([query], false)
}

def setCalcIsEnd = {
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
                            In.builder().field(CardField.STATUS).values(endStatuses).build()
                    ]
                    cardDao.updateByQuery(queries, CardField.CALC_IS_END, true)
                    cardEventDao.setCalcIsEnd(queries, true)
                }
            }
        }
        pageNum++
    }
}

try {
    init()
    setCalcIsEnd()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"