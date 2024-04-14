/**
 * 背景: 详见data/v16
 * 问题：绑定一个删除的计划，导致无法从计划/需求池搜索到；
 * 方案：卡片绑定的计划如果已被删除，迁移到需求池；
 */

import com.ezone.ezproject.dal.mapper.CardMapper
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.query.Ids
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.stream.Collectors

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardMapper = SpringBeanFactory.getBean(CardMapper.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)

def migrateCardDeletedPlanId = {
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
            def plans = planQueryService.selectByProjectId(projectId, null)
            def planIds = plans.stream().map({ it.id }).collect(Collectors.toSet())
            def cards = cardQueryService.selectByProjectId(projectId)
            if (!cards) {
                return
            }
            cards = cards.findAll {it.planId > 0 && !planIds.contains(it.planId)}
            if (!cards) {
                return
            }
            cards.forEach({
                it.planId = 0
                cardMapper.updateByPrimaryKey(it)
            })
            cardDao.updateByQuery(Ids.builder().ids(cards.collect {it.id.toString()}).build(), CardField.PLAN_ID, 0L)
            cardEventDao.setPlanId([Ids.builder().ids(cards.collect {it.latestEventId.toString()}).build()], 0L)
        }
        pageNum++
    }
}

try {
    migrateCardDeletedPlanId()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
