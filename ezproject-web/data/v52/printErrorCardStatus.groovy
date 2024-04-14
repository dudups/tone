/**
 * 20221210
 * 指定卡片类型状态关闭与打开（asyncMigrateForCloseStatus失败），导致状态变更后的后续计算字段没有更新（changeFields:STATUS、BPM_FLOW_ID、BPM_FLOW_TO_STATUS）
 * 设置项目schema下具体卡片类型的状态列表（asyncChangeStatusIsEnd失败），导致项目卡片类型设置结束或取消时，卡片是否结束错误(changeFields:CALC_IS_END)及状态异常
 * 归档或取消（onPlanActive及onPlanInActive失败），导致计划归档但卡片未归档。changeFields:PLAN_IS_ACTIVE
 */


import com.ezone.ezproject.dal.entity.Plan
import com.ezone.ezproject.dal.entity.Project
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.es.entity.CardType
import com.ezone.ezproject.es.entity.ProjectCardSchema
import com.ezone.ezproject.modules.card.bean.CardBean
import com.ezone.ezproject.modules.card.bean.SearchEsRequest
import com.ezone.ezproject.modules.card.bean.query.Query
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.card.service.CardSearchService
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)
def cardSearchService = SpringBeanFactory.getBean(CardSearchService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def printProjectCard = { ProjectCardSchema schema, Project project ->
    def update = false
    def now = new Date()
    if (schema?.types) {
        Map<String, CardType> typeMap = schema.types.collectEntries { [it.key, it] }
        def plans = planQueryService.selectByProjectId(project.getId(), null)
        String[] fields = [CardField.TYPE, CardField.STATUS, CardField.CALC_IS_END, CardField.BPM_FLOW_ID, CardField.PLAN_IS_ACTIVE, CardField.SEQ_NUM, CardField.END_DATE, CardField.LAST_END_TIME, CardField.LAST_END_DELAY, CardField.PLAN_ID]
        SearchEsRequest searchCardRequest = SearchEsRequest.builder().fields(fields).queries(new ArrayList<Query>()).build()
        //处理已经归档的计划
        plans.forEach({ plan ->
            def totalCards = cardSearchService.search(plan, false, searchCardRequest)
            if (totalCards.total > 0) {
                Map<Long, Map<String, Object>> allCards = new HashMap<>()
                totalCards.getList().forEach({ cardBean ->
                    if (FieldUtil.getPlanId(cardBean.getCard()) == plan.getId()) {
                        planCardProcess(cardBean, typeMap, project, plan, now)
                    }
                })
            }
        })
    }
    return update
}

//活跃计划中卡片处理
private HashMap<String, Object> planCardProcess(CardBean cardBean, Map<String, CardType> typeMap, Project project, Plan plan, Date now) {
    def cardDetail = cardBean.getCard()
    def cardType = typeMap.get(FieldUtil.getType(cardDetail))
    def cardStatus = FieldUtil.getStatus(cardDetail)
    def cardIsEnd = FieldUtil.getCalcIsEnd(cardDetail)
    def statusConf = cardType.findStatusConf(cardStatus)
    def bpmFlowId = FieldUtil.getBpmFlowId(cardDetail)
    def planActiveStatus = plan.isActive ? "活动" : "归档"
    Map<String, Object> updateDetail = new HashMap<>()
    if (!statusConf) {
        //状态如何处理，先看有没这样的数据。
        println(String.format("项目[%s]-计划名[%s]-计划状态[%s]-卡片编号[%s],卡片状态[%s]已删除", project.getName(), plan.getName(), planActiveStatus, FieldUtil.getSeqNum(cardDetail), cardStatus))

    } else if (cardIsEnd != statusConf.isEnd) {
        println(String.format("项目[%s]-计划名[%s]-计划状态[%s]-卡片编号[%s]-卡片状态[%s]-是否结束不一致：卡片[%s]，配置[%s]",
                project.getName(), plan.getName(), planActiveStatus, FieldUtil.getSeqNum(cardDetail), cardStatus, cardIsEnd, statusConf.isEnd))

    }
    if (FieldUtil.getPlanIsActive(cardDetail) != plan.getIsActive()) {
        println(String.format("项目[%s]-计划[%s]-卡片编号[%s]-与计划活跃状态不一致",
                project.getName(), plan.getName(), FieldUtil.getSeqNum(cardDetail), FieldUtil.getPlanIsActive(cardDetail)))
        updateDetail.put(CardField.PLAN_IS_ACTIVE, plan.getIsActive())
    }
    return updateDetail
}

def fixProjectSchema = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.each { project ->
            def projectId = project.id
            def schema = schemaDao.find(projectId)
            if (printProjectCard(schema, project)) {
                println "projectId=${projectId}"
            }
        }
        pageNum++
    }
}
try {
    fixProjectSchema()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"