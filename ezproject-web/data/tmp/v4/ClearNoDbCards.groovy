/**
 * 创建卡片已经入ES，但后续如清理草稿，遇到RST异常等导致DB回滚，导致残留历史数据，工作台这种场景直接搜ES有，但是按有限活跃计划先走DB搜的接口搜不到
 */

import com.ezone.ezproject.dal.entity.CardExample
import com.ezone.ezproject.dal.mapper.CardMapper
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.modules.attachment.service.CardAttachmentCmdService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.collections4.ListUtils
import org.apache.commons.lang.exception.ExceptionUtils

def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def cardMapper = SpringBeanFactory.getBean(CardMapper.class)
def cardAttachmentCmdService = SpringBeanFactory.getBean(CardAttachmentCmdService.class)

def clear = {
    ListUtils.partition(cardDao.searchIds(), 1000).each {
        def example = new CardExample()
        example.createCriteria().andIdIn(it)
        def ids = cardMapper.selectByExample(example).collect {it.id}.toSet()
        def clearIds = it.findAll {!ids.contains(it)}
        if (clearIds) {
            println clearIds
            cardDao.delete(clearIds)
            cardEventDao.deleteByCardIds(clearIds)
            cardAttachmentCmdService.delete(0, clearIds);
        }
    }
}

try {
    clear()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
