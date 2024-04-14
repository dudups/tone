import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.dao.ProjectExtendDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.es.entity.ProjectField
import com.ezone.ezproject.modules.card.bean.query.Ids
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def projectExtendDao = SpringBeanFactory.getBean(ProjectExtendDao.class)

def init = {
    projectExtendDao.updateByQuery([], ProjectField.IS_ACTIVE, true)
    def ids = []
    cardEventDao.scrollAll(cardEventDao.index(), [], {hit ->
        ids.add(hit.id)
        if (ids.size() >= 1000) {
            cardEventDao.updateByQuery([Ids.builder().ids(ids).build()], CardField.PROJECT_IS_ACTIVE, true)
            ids = []
        }
    })
    if (ids.size() >= 0) {
        cardEventDao.updateByQuery([Ids.builder().ids(ids).build()], CardField.PROJECT_IS_ACTIVE, true)
        ids = []
    }
    cardDao.scrollAll(cardDao.index(), [], {hit ->
        ids.add(hit.id)
        if (ids.size() >= 1000) {
            cardDao.updateByQuery([Ids.builder().ids(ids).build()], CardField.PROJECT_IS_ACTIVE, true)
            ids = []
        }
    })
    if (ids.size() >= 0) {
        cardDao.updateByQuery([Ids.builder().ids(ids).build()], CardField.PROJECT_IS_ACTIVE, true)
        ids = []
    }
}
try {
    init()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"