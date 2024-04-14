/**
 * 20220711
 * 由于部份历史卡片事件缺少InnerType，故此需要补上此字此字段
 * 由于部份项目数据量大5秒执行不完，加大es更新的等待时间。
 */

import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.card.bean.query.NotExist
import com.ezone.ezproject.modules.card.bean.query.Query
import com.ezone.ezproject.modules.card.event.model.CardEvent
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)

def setInnerTypeIfNotExist = {
    def types = ["task", "epic", "feature", "story", "bug", "transaction", "custom_1", "custom_2", "custom_3", "custom_4", "custom_5"]
    def innerTypes = ["task", "requirement", "requirement", "requirement", "bug", "transaction", "bug", "bug", "task", "task", "transaction"]
    for (i in 0..<11) {
        List<Query> list = new ArrayList<>()
        list.add(NotExist.builder().field(CardEvent.cardProp(CardField.INNER_TYPE)).build())
        list.add(Eq.builder().field(CardEvent.cardProp(CardField.TYPE)).value(types[i]).build())
        cardEventDao.updateByQuery(list, CardField.INNER_TYPE, innerTypes[i], 120)
        println("处理" + types[i] + "完成")
    }
}

try {
    setInnerTypeIfNotExist()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"