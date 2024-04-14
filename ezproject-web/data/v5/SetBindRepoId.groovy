/**
 * 20210825:
 * 项目关联代码库从关联路径改为关联ID，刷历史数据；
 */

import com.ezone.devops.ezcode.sdk.service.impl.InternalRepoServiceImpl
import com.ezone.ezproject.dal.mapper.ProjectRepoMapper
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.project.service.ProjectRepoService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def projectRepoMapper = SpringBeanFactory.getBean(ProjectRepoMapper.class)
def repoService = SpringBeanFactory.getBean(InternalRepoServiceImpl.class)
def projectRepoService = SpringBeanFactory.getBean(ProjectRepoService.class)

def setBindRepoId = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.forEach {
            def projectId = it.id
            def projectRepos = projectRepoService
                    .selectByProjectId(projectId)
                    .findAll({it.repoId == 0})
            if (!projectRepos) {
                return
            }
            def repos = repoService
                    .listReposByNames(it.companyId, projectRepos.collect({it.repo}))
                    .collectEntries({[it.getRepoName(), it.id]})
            projectRepos.forEach({
                def repoId = repos.get(it.repo)
                if (repoId) {
                    it.setRepoId(repoId)
                    projectRepoMapper.updateByPrimaryKey(it)
                } else {
                    projectRepoMapper.deleteByPrimaryKey(it.id)
                }
            })
        }
        pageNum++
    }
}

try {
    setBindRepoId()
} catch (Throwable e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"
