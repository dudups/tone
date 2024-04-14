import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.modules.chart.controller.ChartController
import com.ezone.ezproject.modules.chart.service.ChartDataService
import com.ezone.ezproject.modules.chart.service.ChartQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def chartController = SpringBeanFactory.getBean(ChartController.class)
def chartDataService = SpringBeanFactory.getBean(ChartDataService.class)
def chartQueryService = SpringBeanFactory.getBean(ChartQueryService.class)


def debug = {
    def chart = chartQueryService.selectProjectChart(732743979687297024L)
    println chartDataService.chartData(chart.projectId, chart.id)
}

try {
    debug()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
