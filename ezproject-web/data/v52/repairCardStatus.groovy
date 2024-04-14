/**
 * 20221210
 * 指定卡片类型状态关闭与打开（asyncMigrateForCloseStatus失败），导致状态变更后的后续计算字段没有更新（changeFields:STATUS、BPM_FLOW_ID、BPM_FLOW_TO_STATUS）
 * 设置项目schema下具体卡片类型的状态列表、包含设置结束状态（asyncChangeStatusIsEnd失败），导致项目卡片类型设置结束或取消时，卡片是否结束错误(changeFields:CALC_IS_END)及状态异常
 * 归档或取消（onPlanActive及onPlanInActive失败），导致计划归档但卡片未归档。changeFields:PLAN_IS_ACTIVE
 */


import com.ezone.ezproject.common.IdUtil
import com.ezone.ezproject.dal.entity.Plan
import com.ezone.ezproject.dal.entity.Project
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.SearchEsRequest
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.card.bean.query.In
import com.ezone.ezproject.modules.card.event.model.CardEvent
import com.ezone.ezproject.modules.card.event.model.EventType
import com.ezone.ezproject.modules.card.event.service.CardEventCmdService
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.card.service.CardSearchService
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.stream.Collectors

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def cardSearchService = SpringBeanFactory.getBean(CardSearchService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventCmdService = SpringBeanFactory.getBean(CardEventCmdService.class)

def processPlanInactiveError = { Project project ->
    //处理归档计划
    List<Plan> inactivePlans = planQueryService.selectByProjectId(project.getId(), false)
    if (CollectionUtils.isEmpty(inactivePlans)) {
        return
    }
    def inactivePlanIds = inactivePlans.stream().map({ it.getId().toString() }).collect(Collectors.toList())
    Map<Long, Plan> planMap = inactivePlans.collectEntries({ [it.getId(), it] })
    def activeQueries = Arrays.asList(In.builder().field(CardField.PLAN_ID).values(inactivePlanIds).build(),
            Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(project.getId())).build(),
            Eq.builder().field(CardField.PLAN_IS_ACTIVE).value(String.valueOf(true)).build()
    )
    SearchEsRequest inActivePlan = SearchEsRequest.builder()
            .queries(activeQueries)
            .fields(null)
            .build()
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def planErrCards = cardSearchService.search(inActivePlan, pageNum, pageSize)
        if (CollectionUtils.isEmpty(planErrCards.getList())) {
            break
        }
        cardSearchService.search(inActivePlan, pageNum, pageSize)
        List<Long> errCardIds = planErrCards.getList().stream().map({ cardBean -> cardBean.getId() }).collect(Collectors.toList())
        StringBuilder sb = new StringBuilder()
//        println("卡片未归档，计划已归档的卡片:")
        List<CardEvent> cardEvents = new ArrayList<>();
        planErrCards.getList().forEach({ cardBean ->
            def cardDetail = cardBean.getCard()
            def planId = FieldUtil.getPlanId(cardDetail)
            if (planMap != null) {
//                sb.append("项目[").append(project.getName() + "-" + project.getId()).append("]-").append("计划[").append(planMap?.get(planId)?.getName()).append("]-卡片[")
//                        .append(FieldUtil.getSeqNum(cardDetail)).append("]\n")
                def plan = planMap?.get(planId)
                if (plan != null) {
                    cardDetail.put(CardField.PLAN_IS_ACTIVE, false)
                    cardEvents.add(CardEvent.builder()
                            .id(IdUtil.generateId())
                            .cardId(cardBean.id)
                            .date(plan.getLastModifyTime())
                            .user(plan.getLastModifyUser())
                            .eventType(EventType.PLAN_IS_ACTIVE)
                            .cardDetail(cardDetail)
                            .build())
                }
            }
        })
//        println(sb)
        Map<String, Object> cardUpdatePro = new HashMap<>()
        cardUpdatePro.put(CardField.PLAN_IS_ACTIVE, false)
        cardDao.updateSelective(errCardIds, cardUpdatePro)
//        println(cardEvents.size() + "个事件更新" + CardEventDao.JSON_MAPPER.writeValueAsString(cardEvents.get(0)))
        cardEventCmdService.asyncSave(cardEvents)
        pageNum++
    }
}


def fixProjectCard = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        List<Project> projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.each { project ->
            processPlanInactiveError(project)
        }
        pageNum++
    }
}

try {
    fixProjectCard()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"