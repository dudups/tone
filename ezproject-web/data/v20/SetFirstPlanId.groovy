/**
 * 20220331:
 * 项目下卡片因添加冗余字段first_plan_id辅助判断是否跨迭代，刷历史数据
 */
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def setFirstPlanId = {
    def pageNum = 1
    def pageSize = 200
    while (true) {
        def cards = cardQueryService.selectAll(pageNum, pageSize)
        if (!cards) {
            break
        }
        def cardIds = cards.collect {it.id}
        def cardEvents = cardEventDao.searchForChart(cardIds, [CardField.PLAN_ID])
        def cardsProps = new HashMap()
        cardEvents
                .findAll { FieldUtil.getPlanId(it.cardDetail) > 0}
                .groupBy {it.cardId}
                .entrySet()
                .forEach {
                    def cardProps = new HashMap()
                    cardProps.put(CardField.FIRST_PLAN_ID, FieldUtil.getPlanId(it.value.min { it.date}.cardDetail))
                    cardsProps.put(it.key, cardProps)
                }
        if (cardsProps) {
            cardDao.updateSelective(cardsProps)
        }
        pageNum++
    }
}

try {
    setFirstPlanId()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"