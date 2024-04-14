/**
 * 背景: 详见data/v8
 * 问题：自定义报表，对于历史归档的卡片统计数据不对；
 * 方案：计划归档/激活操作，生成卡片事件；对于当前已归档计划的卡片，按计划的最近修改时间生成一次卡片事件；
 */
import com.ezone.ezproject.common.IdUtil
import com.ezone.ezproject.dal.entity.Card
import com.ezone.ezproject.dal.mapper.CardMapper
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardMapper = SpringBeanFactory.getBean(CardMapper.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)

def createEventForPlanIsActive = {
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
            def plans = planQueryService.selectByProjectId(projectId, false)
            if (!plans) {
                return
            }
            plans.forEach {plan ->
                def cards = cardQueryService.selectByPlanId(plan.id)
                if (!cards) {
                    return
                }

                def latestEvents = cardEventDao.find(cards.collect{it.latestEventId})
                if (!latestEvents) {
                    return
                }

                Map<Long, Card> cardMap = cards.collectEntries {[it.id, it]}
                def eventIds = []
                Map<Long, String> cardEventsJsons = new HashMap<>();
                latestEvents.forEach {event ->
                    if (event.date.before(plan.lastModifyTime)) {
                        eventIds.add(event.id)

                        def id = IdUtil.generateId()
                        def card = cardMap.get(event.cardId)
                        card.latestEventId = id
                        cardMapper.updateByPrimaryKey(card)

                        event.id = id
                        event.date = plan.lastModifyTime
                        event.cardDetail.put(CardField.PLAN_IS_ACTIVE, false)
                        cardEventsJsons.put(id, CardEventDao.JSON_MAPPER.writeValueAsString(event))
                    }
                }

                if (eventIds) {
                    cardEventDao.setNextTime(eventIds, plan.lastModifyTime)
                }
                if (cardEventsJsons) {
                    cardEventDao.saveOrUpdate(cardEventsJsons)
                }
            }
        }
        pageNum++
    }
}

try {
    createEventForPlanIsActive()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
