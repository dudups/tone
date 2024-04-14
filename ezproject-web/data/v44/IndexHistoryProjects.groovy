/**
 * 20221219
 * 项目建立完整索引，db的基本信息也放到es project-extend中，刷历史数据。
 */

import com.ezone.ezproject.es.dao.ProjectExtendDao
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.project.util.ProjectUtil
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def projectExtendDao = SpringBeanFactory.getBean(ProjectExtendDao.class)

def init = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        def exts = projectExtendDao.findAsMap(projects.collect {it.id})
        projects.each {project ->
            exts.put(project.id, ProjectUtil.projectIndexMap(project, exts.get(project.id)))
        }
        projectExtendDao.saveOrUpdate(exts)
        pageNum++
    }
}

try {
    init()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"