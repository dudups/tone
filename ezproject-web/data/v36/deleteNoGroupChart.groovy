/**
 * 20220927
 * 删除项目报表中分组为0的报表。
 */

import com.ezone.ezproject.dal.entity.ProjectChartExample
import com.ezone.ezproject.dal.mapper.ProjectChartMapper
import com.ezone.ezproject.es.dao.ChartDao
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.stream.Collectors

def chartDao = SpringBeanFactory.getBean(ChartDao.class)
def projectChartMapper = SpringBeanFactory.getBean(ProjectChartMapper.class)

def deleteProjectChartDirty = {
    ProjectChartExample example = new ProjectChartExample();
    example.createCriteria().andGroupIdEqualTo(0L)
    def errorCharts = projectChartMapper.selectByExample(example)
    if (errorCharts != null) {
        println("脏数据：" + errorCharts.size())
        def ids = errorCharts.stream().map({ chart -> chart.getId() }).collect(Collectors.toList())
        ids.stream().forEach({
            projectChartMapper.deleteByPrimaryKey(it)
            chartDao.delete(it)
        })
    }
}

try {
    deleteProjectChartDirty()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"