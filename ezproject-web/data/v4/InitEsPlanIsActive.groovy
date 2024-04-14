/**
 * 20210802:
 * 根据绑定plan生成card的plan_is_active字段冗余信息；
 */
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.card.bean.query.In
import com.ezone.ezproject.modules.card.bean.query.NotEq
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)

def init = {
    def query = NotEq.builder().field(CardField.PLAN_IS_ACTIVE).value("true").build()
    cardDao.updateByQuery(query, CardField.PLAN_IS_ACTIVE, false)
}

def setPlanIsActive = {
    // 需求池
    def query = Eq.builder().field(CardField.PLAN_ID).value("0").build()
    cardDao.updateByQuery(query, CardField.PLAN_IS_ACTIVE, true)
    // 活跃计划
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.forEach {
            def projectId = it.id
            def plans = planQueryService.selectByProjectId(projectId, true)
            if (!plans) {
                return
            }
            def queries = [
                    In.builder().field(CardField.PLAN_ID).values(plans.collect {it.id}).build()
            ]
            cardDao.updateByQuery(queries, CardField.PLAN_IS_ACTIVE, true)
        }
        pageNum++
    }
}

try {
    init()
    setPlanIsActive()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
