/**
 * 20220708:
 * 状态流目标状态有重复，原来是兼容，后因审计逻辑toMap时异常导致设置状态流异常，刷下历史数据去掉重复
 * 20220905:
 * 字段定义有重复，加上项目模版角度，各种其它角度的重复修复，同时加上@Uniq注解校验（在字段联动分支上实现，等合并后加上）
 */

import com.ezone.ezproject.dal.entity.ProjectTemplateExample
import com.ezone.ezproject.dal.mapper.ProjectTemplateMapper
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao
import com.ezone.ezproject.es.dao.ProjectTemplateDetailDao
import com.ezone.ezproject.es.entity.ProjectCardSchema
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)
def templateMapper = SpringBeanFactory.getBean(ProjectTemplateMapper.class)
def templateDetailDao = SpringBeanFactory.getBean(ProjectTemplateDetailDao.class)

def fixSchema = { ProjectCardSchema schema ->
    def update = false
    if (schema?.fields) {
        def fields = schema.fields.collectEntries {[it.key, it]}.values().toList()
        if (schema.fields.size() > fields.size()) {
            schema.fields = fields
            update = true
        }
        def statuses = schema.statuses.collectEntries {[it.key, it]}.values().toList()
        if (schema.statuses.size() > statuses.size()) {
            schema.statuses = statuses
            update = true
        }
        def types = schema.types.collectEntries {[it.key, it]}.values().toList()
        if (schema.types.size() > types.size()) {
            schema.types = types
            update = true
        }
        schema.getTypes().each {
            def typeFields = it.fields.collectEntries {[it.key, it]}.values().toList()
            if (it.fields.size() > typeFields.size()) {
                it.fields = typeFields
                update = true
            }
            def typeStatuses = it.statuses.collectEntries {[it.key, it]}.values().toList()
            if (it.statuses.size() > typeStatuses.size()) {
                it.statuses = typeStatuses
                update = true
            }
            it.statuses.each {
                def statusFlows = it.statusFlows.collectEntries {[it.targetStatus, it]}.values().toList()
                if (it.statusFlows.size() > statusFlows.size()) {
                    it.statusFlows = statusFlows
                    update = true
                }
            }
        }
    }
    return update
}

def fixProjectSchema = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.each {
            def projectId = it.id
            def schema = schemaDao.find(projectId)
            if (fixSchema(schema)) {
                println "projectId=${projectId}"
                schemaDao.saveOrUpdate(projectId, schema)
            }
        }
        pageNum++
    }
}

def fixTemplateSchema = {
    templateMapper.selectByExample(new ProjectTemplateExample()).each {
        def templateId = it.id
        def detail = templateDetailDao.find(templateId)
        if (fixSchema(detail.projectCardSchema)) {
            println "templateId=${templateId}"
            templateDetailDao.saveOrUpdate(templateId, detail)
        }
    }
}

try {
    fixProjectSchema()
    fixTemplateSchema()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"