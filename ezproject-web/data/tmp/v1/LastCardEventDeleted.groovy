/**
 * 20210814:
 * 原因：复制卡片树删除源卡片部分场景卡片设置未已删除，但卡片事件未中卡片冗余字段未被标记删除，此脚本刷脏数据；
 */

import com.ezone.ezproject.dal.entity.CardExample
import com.ezone.ezproject.dal.mapper.CardMapper
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.modules.card.bean.query.Ids
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import com.github.pagehelper.PageHelper
import org.apache.commons.lang.exception.ExceptionUtils

def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def cardMapper = SpringBeanFactory.getBean(CardMapper.class)

def setEventDeleted = {
    def example = new CardExample()
    example.createCriteria().andDeletedEqualTo(true)
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        PageHelper.startPage(pageNum, pageSize, false)
        def cards = cardMapper.selectByExample(example)
        if (!cards) {
            break
        }
        def query = Ids.builder().ids(cards.collect{it.latestEventId.toString()}).build()
        cardEventDao.setDeleted([query], true)
        pageNum++
    }
}

try {
    setEventDeleted()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
