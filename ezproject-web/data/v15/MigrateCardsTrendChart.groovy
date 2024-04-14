/**
 * 20220307:
 * 项目自定义报表卡片趋势图升级，刷历史数据为新数据结构；
 */

import com.ezone.ezproject.dal.entity.ProjectChartExample
import com.ezone.ezproject.dal.mapper.ProjectChartMapper
import com.ezone.ezproject.es.dao.ChartDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.chart.config.CardsMultiTrend
import com.ezone.ezproject.modules.chart.config.CardsTrend
import com.ezone.ezproject.modules.chart.config.Chart
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import com.github.pagehelper.PageHelper
import org.apache.commons.lang.exception.ExceptionUtils

def chartMapper = SpringBeanFactory.getBean(ProjectChartMapper.class)
def chartDao = SpringBeanFactory.getBean(ChartDao.class)

def query = Eq.builder().field(CardField.CALC_IS_END).value("true").build()

def migrateCardsTrendChart = {
    def pageNum = 1
    def pageSize = 1000
    def example = new ProjectChartExample()
    example.createCriteria().andTypeEqualTo("cardsTrend")
    while (true) {
        PageHelper.startPage(pageNum, pageSize, false)
        def charts = chartMapper.selectByExample(example)
        if (!charts) {
            break
        }
        charts.forEach {
            it.type = "cardsMultiTrend"
            chartMapper.updateByPrimaryKey(it)
            def chartId = it.id
            def chart = chartDao.find(chartId)
            def queries = [query]
            if (chart.queries) {
                queries.addAll(chart.queries)
            }
            def fromConfig = (CardsTrend) chart.config
            def toConfig = CardsMultiTrend.builder()
                    .dateInterval(fromConfig.dateInterval)
                    .dateRange(fromConfig.dateRange)
                    .yConfigs([
                            CardsMultiTrend.YConfig.builder()
                                    .name("完成卡片数")
                                    .queries(queries)
                                    .build(),
                            CardsMultiTrend.YConfig.builder()
                                    .name("完成卡片数-增量")
                                    .queries(queries)
                                    .deltaMetric(true)
                                    .build()
                    ])
                    .build()
            chartDao.saveOrUpdate(chartId, Chart.builder()
                    .config(toConfig)
                    .build())
        }
        pageNum++
    }
}

try {
    migrateCardsTrendChart()
} catch (Throwable e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
