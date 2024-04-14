/**
 * 20221124
 * 处理卡片逻辑删除时，未删除mysql中关联关系的历史数据
 */


import com.ezone.ezproject.dal.entity.CardExample
import com.ezone.ezproject.dal.entity.CardRelateRel
import com.ezone.ezproject.dal.entity.CardRelateRelExample
import com.ezone.ezproject.dal.entity.ProjectChartExample
import com.ezone.ezproject.dal.mapper.CardMapper
import com.ezone.ezproject.dal.mapper.ProjectChartMapper
import com.ezone.ezproject.es.dao.ChartDao
import com.ezone.ezproject.modules.card.service.CardRelateRelCmdService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import com.github.pagehelper.PageHelper
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.stream.Collectors


def cardMapper = SpringBeanFactory.getBean(CardMapper.class)
def cardRelateRelCmdService = SpringBeanFactory.getBean(CardRelateRelCmdService.class)

def deleteCardRelate = {
    def page = 1
    def pageSize = 200
    while (true) {
        def cardExample = new CardExample()
        cardExample.createCriteria().andDeletedEqualTo(true)
        PageHelper.startPage(page, pageSize)
        def cards = cardMapper.selectByExample(cardExample)
        if (!cards) {
            break
        }
        cardRelateRelCmdService.deleteAll(cards.stream().map({card -> card.getId()}).collect(Collectors.toList()))
        page++
    }
}

try {
    deleteCardRelate()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"