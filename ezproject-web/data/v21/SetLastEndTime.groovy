/**
 * 20220331:
 * 项目下卡片因添加冗余字段first_plan_id辅助判断是否跨迭代，刷历史数据
 */
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.event.model.CardEvent
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.function.BinaryOperator

def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def setLastEndTime = {
    def pageNum = 1
    def pageSize = 200
    while (true) {
        def cards = cardQueryService.selectAll(pageNum, pageSize)
        if (!cards) {
            break
        }
        def cardIds = cards.collect { it.id }
        def cardEvents = cardEventDao.searchForChart(cardIds, [CardField.CALC_IS_END])
        def cardsProps = new HashMap()
        cardEvents
                .groupBy { it.cardId }
                .entrySet()
                .forEach {
                    def eventsList = it.value
                    Date lastEndTime;
                    def isEnd = false;
                    def size = eventsList.size()
                    for (int i = 0; i < size; i++) {
                        def currentEvent = eventsList.get(i)
                        if (currentEvent.getCardDetail()) {
                            def currentIsEnd = FieldUtil.getCalcIsEnd(currentEvent.getCardDetail())
                            if (!isEnd && currentIsEnd) {
                                lastEndTime = currentEvent.getDate();
                            }
                            isEnd = currentIsEnd
                        }
                        preCardEvent = currentEvent
                    }
                    if (lastEndTime) {
                        def cardProps = new HashMap()
                        cardProps.put(CardField.LAST_END_TIME, lastEndTime.getTime())
                        cardsProps.put(it.key, cardProps)
                    }
                }
        if (cardsProps.size() > 0) {
            cardDao.updateSelective(cardsProps)
        }
        pageNum++
    }
}

try {
    setLastEndTime()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"