/**
 * 20220514:
 * 查到游离的计划断片，挂到最近的祖先计划上，并改为非活跃计划，卡片冗余字段也同步改
 */
import com.ezone.ezproject.dal.mapper.PlanMapper
import com.ezone.ezproject.modules.card.service.CardCmdService
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.ezproject.modules.card.service.CardSearchService
import com.ezone.ezproject.modules.plan.service.RepairPlanHelper
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def planMapper = SpringBeanFactory.getBean(PlanMapper.class)
def cardCmdService = SpringBeanFactory.getBean(CardCmdService.class)
def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardSearchService = SpringBeanFactory.getBean(CardSearchService.class)

def repairPlan = {
    def helper = RepairPlanHelper.builder().planMapper(planMapper).cardCmdService(cardCmdService).cardQueryService(cardQueryService).cardSearchService(cardSearchService).build()
    helper.processAncestorsDeleteOrInactive();
}

try {
    repairPlan()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"