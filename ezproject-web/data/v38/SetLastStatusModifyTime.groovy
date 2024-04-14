/**
 * 202211.19
 * 新增状态的最后修改时间，用于统计状态卡片停留时间
 */

import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.event.model.CardEvent
import com.ezone.ezproject.modules.card.event.model.EventType
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils
import org.elasticsearch.search.sort.SortOrder

def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def count = 0
def setLastStatusModifyTime = {
    def pageNum = 1
    def pageSize = 200
    while (true) {
        count++
        def cards = cardQueryService.selectAll(pageNum, pageSize)
        if (!cards) {
            break
        }
        def cardIds = cards.collect { it.id }
        def cardEvents = cardEventDao.searchEvent(cardIds, ["id", "eventType", "eventMsg", "date", "cardId"], [], CardEvent.DATE, SortOrder.ASC)
        def cardsProps = new HashMap()
        cardEvents.groupBy { it.cardId }
                .entrySet()
                .forEach {
                    def cardProps = new HashMap()
                    cardsProps.put(it.key, cardProps)
                    def lastStatusModifyTime = null
                    it.value.forEach({
                        switch (it.getEventType()) {
                            case EventType.CREATE:
                                lastStatusModifyTime = it.getDate()
                                break
                            case EventType.UPDATE:
                                def detailMsgs = ((UpdateEventMsg) it.getEventMsg()).fieldDetailMsgs
                                if (detailMsgs) {
                                    if (detailMsgs.stream().anyMatch({ (it.fieldKey == CardField.STATUS) })) {
                                        lastStatusModifyTime = it.getDate()
                                    }
                                }
                                break
                            case EventType.AUTO_STATUS_FLOW:
                                lastStatusModifyTime = it.getDate()
                                break
                            default:
                                break
                        }
                    })
                    if (lastStatusModifyTime != null) {
                        cardProps.put(CardField.LAST_STATUS_MODIFY_TIME, lastStatusModifyTime.getTime())
                    }
                }
        if (cardsProps) {
            cardDao.updateSelective(cardsProps)
        }
        pageNum++
    }
}

try {
    setLastStatusModifyTime()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"