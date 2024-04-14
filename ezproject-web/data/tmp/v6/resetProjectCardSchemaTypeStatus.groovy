/**
 * 20220830
 * 项目schema状态与卡片中状态不一致(卡片中状态有总状态不存在的值），导至相关操作失败。
 */


import com.ezone.ezproject.dal.entity.ProjectTemplateExample
import com.ezone.ezproject.dal.mapper.ProjectTemplateMapper
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao
import com.ezone.ezproject.es.dao.ProjectTemplateDetailDao
import com.ezone.ezproject.es.entity.CardType
import com.ezone.ezproject.es.entity.ProjectCardSchema
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaSettingHelper
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.function.Function
import java.util.stream.Collectors

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaSettingHelper = SpringBeanFactory.getBean(ProjectCardSchemaSettingHelper.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)
def templateMapper = SpringBeanFactory.getBean(ProjectTemplateMapper.class)
def templateDetailDao = SpringBeanFactory.getBean(ProjectTemplateDetailDao.class)


def fixTypeStatus = { ProjectCardSchema schema ->
    def needDeleteKey = false
    Set<String> needDeleteKeys = new HashSet<>()
    def statusKeys = schema.getStatuses().stream().map({ cardStatus -> cardStatus.getKey() }).collect(Collectors.toSet())
    schema.getTypes().forEach({ cardType ->
        if (cardType.getStatuses()) {
            List<CardType.StatusConf> statusConfigs = cardType.getStatuses().stream().filter({ statusConf ->
                if (statusKeys.contains(statusConf.getKey())) {
                    def toStatusKeys = new HashSet()
                    def flowConfigs = statusConf.getStatusFlows().stream().filter({ flowConfig ->
                        //去掉重复的toStatus
                        if (toStatusKeys.contains(flowConfig.getTargetStatus())) {
                            needDeleteKey = true
                            println("错误信息[重复key，cardType:" + cardType.getKey() + "中的status.statusFlows:" + statusConf.key
                                    + "有错误的targetStatus:" + flowConfig.getTargetStatus() + "]")
                            return false
                        }
                        toStatusKeys.add(flowConfig.getTargetStatus())

                        //自己到自己
                        if (statusConf.getKey() == flowConfig.getTargetStatus()) {
                            println("错误信息[cardType:" + cardType.getKey() + "中的status.statusFlows:" + statusConf.key
                                    + "有错误的targetStatus:" + flowConfig.getTargetStatus() + "]")
                            needDeleteKey = true
                            return false
                        }

                        //去掉非schema状态列表中的flow
                        if (!statusKeys.contains(flowConfig.getTargetStatus())) {
                            //处理模板中/type/statuses/statusFlows中相对/statuses中多余的key
                            println("错误信息[cardType:" + cardType.getKey() + "中的status.statusFlows:" + statusConf.key
                                    + "有错误的targetStatus:" + flowConfig.getTargetStatus() + "]")
                            needDeleteKey = true
                            return false
                        }

                        return true
                    }).collect(Collectors.toList())
                    statusConf.setStatusFlows(flowConfigs)
                    return true
                } else {
                    //去掉type/statuses中非schema中状态statuses中的key。
                    println("错误信息:" + ",cardType:" + cardType.getKey() + "中有多余的status:" + statusConf.key)
                    needDeleteKeys.add(statusConf.getKey())
                    needDeleteKey = true
                    return false
                }
            }).collect(Collectors.toList())
            cardType.setStatuses(statusConfigs)
        }
    })
    return needDeleteKey
}


def fixProjectSchema = { Function<ProjectCardSchema, Boolean> function ->
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
            if (function.apply(schema)) {
                println "projectId=${projectId}"
                schemaDao.saveOrUpdate(projectId, schema)
            }
        }
        pageNum++
    }
}
//
def fixTemplateSchema = { Function<ProjectCardSchema, Boolean> function ->
    templateMapper.selectByExample(new ProjectTemplateExample()).each {
        def templateId = it.id
        def detail = templateDetailDao.find(templateId)
        if (function.apply(detail.projectCardSchema)) {
            println "templateId=${templateId}"
            templateDetailDao.saveOrUpdate(templateId, detail)
        }
    }
}

try {
    fixProjectSchema(fixTypeStatus)
    fixTemplateSchema(fixTypeStatus)
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"