/**
 * 20220209 项目字段选项，添加key标识，历史数据：生成字段标识和刷卡片历史数据（卡片，事件，卡片查询管理，字段触发器（项目+模版），报表（项目+insight））；
 * 更近一步，顺便把用户自定义枚举字段的重复值去重下；
 */

import com.ezone.ezproject.dal.entity.ProjectTemplateExample
import com.ezone.ezproject.dal.mapper.ProjectTemplateMapper
import com.ezone.ezproject.es.dao.*
import com.ezone.ezproject.es.entity.*
import com.ezone.ezproject.es.util.EsIndexUtil
import com.ezone.ezproject.modules.card.bean.query.Eq
import com.ezone.ezproject.modules.card.bean.query.Query
import com.ezone.ezproject.modules.card.event.model.CardEvent
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.chart.config.CardsMultiTrend
import com.ezone.ezproject.modules.chart.ezinsight.config.EzInsightChart
import com.ezone.ezproject.modules.chart.service.ChartQueryService
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.query.service.CardQueryViewQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.exception.ExceptionUtils

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)
def schemaHelper = SpringBeanFactory.getBean(ProjectCardSchemaHelper.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)

def cardQueryViewQueryService= SpringBeanFactory.getBean(CardQueryViewQueryService.class)
def cardQueryViewDao = SpringBeanFactory.getBean(CardQueryViewDao.class)

def chartQueryService = SpringBeanFactory.getBean(ChartQueryService.class)
def chartDao = SpringBeanFactory.getBean(ChartDao.class)

def templateMapper = SpringBeanFactory.getBean(ProjectTemplateMapper.class)
def templateDetailDao = SpringBeanFactory.getBean(ProjectTemplateDetailDao.class)


def getFieldOptionNameKeyMap = {ProjectCardSchema schema ->
    schema?.fields?.findAll { it?.options?.size() > 0 }.collectEntries() {
        [
                it.key,
                it?.options?.collectEntries {[it.name, it.key] }
        ]
    }
}

def getValue = {Object key, Map map ->
    if (key instanceof String) {
        return StringUtils.defaultString(map[key], key)
    } else if (key instanceof Collection) {
        return (key as Collection).collect {StringUtils.defaultString(map[it], it) }
    }
    return key
}

def updateCard = { Long projectId, Map fieldOptionNameKeyMap ->
    Map<Long, Map<String, Object>> cardsProps = [:]
    cardDao.scrollAll(cardDao.index(), [Eq.builder().field(CardField.PROJECT_ID).value("$projectId").build()], {hit ->
        Map<String, Object> cardDetail = hit.getSourceAsMap()
        def cardProps = cardDetail.entrySet()
                .findAll {fieldOptionNameKeyMap.containsKey(it.key) && !FieldUtil.isEmptyValue(it.value)}
                .collectEntries{[it.key, getValue(it.value, fieldOptionNameKeyMap[it.key])]}
        if (cardProps.size() > 0) {
            cardsProps[hit.id.toLong()] = cardProps
        }
        if (cardsProps.size() >= 1000) {
            cardDao.updateSelective(cardsProps)
            cardsProps = [:]
        }
    }, fieldOptionNameKeyMap.keySet() as String[])
    if (cardsProps.size() > 0) {
        cardDao.updateSelective(cardsProps)
    }
}

def updateCardEvent = { Long projectId, Map fieldOptionNameKeyMap ->
    Map<Long, Map<String, Map<String, Object>>> eventProps = [:]
    cardEventDao.scrollAll(cardEventDao.index(), [Eq.builder().field(CardEvent.cardProp(CardField.PROJECT_ID)).value("$projectId").build()], {hit ->
        Map<String, Object> cardDetail = hit.getSourceAsMap()['cardDetail']
        if (cardDetail) {
            def cardProps = cardDetail.entrySet()
                    .findAll {fieldOptionNameKeyMap.containsKey(it.key) && !FieldUtil.isEmptyValue(it.value)}
                    .collectEntries{[it.key, getValue(it.value, fieldOptionNameKeyMap[it.key])]}
            if (cardProps.size() > 0) {
                eventProps[hit.id.toLong()] = [cardDetail: cardProps]
            }
        }
        if (eventProps.size() >= 1000) {
            cardEventDao.updateDoc(cardEventDao.index(), eventProps)
            eventProps = [:]
        }
    }, fieldOptionNameKeyMap.keySet().collect {"cardDetail.$it"} as String[])
    if (eventProps.size() > 0) {
        cardEventDao.updateDoc(cardEventDao.index(), eventProps)
    }
}

def updateFieldFlows = {List<CardFieldFlow> fieldFlows, Map fieldOptionNameKeyMap ->
    fieldFlows?.each {CardFieldFlow fieldFlow ->
        def optionNameKeyMap = fieldOptionNameKeyMap[fieldFlow.fieldKey]
        fieldFlow?.flows?.each { CardFieldValueFlow flow ->
            if (optionNameKeyMap) {
                flow.fieldValue = getValue(flow.fieldValue, optionNameKeyMap)
            }
            flow?.targetFieldValues?.each { CardFieldValue target ->
                def targetOptionNameKeyMap = fieldOptionNameKeyMap[target.fieldKey]
                if (targetOptionNameKeyMap) {
                    target.fieldValue = getValue(target.fieldValue, targetOptionNameKeyMap)
                }
            }
        }
    }
}

def catchRun = {Closure closure ->
    try {
        closure.run()
    } catch (Throwable e) {
        // do nothing
    }
}

def updateQuery = { Query query, Map fieldOptionNameKeyMap ->
    def field = null
    catchRun {field = query['field']}
    if (field) {
        def optionNameKeyMap = fieldOptionNameKeyMap[field]
        if (optionNameKeyMap) {
            def value = null
            catchRun {value = query['value']}
            if (value instanceof String) {
                query['value'] = getValue(value, optionNameKeyMap)
            }
            def values = null
            catchRun {values = query['values']}
            if (values instanceof Collection) {
                query['values'] = (values as Collection).collect {getValue(it, optionNameKeyMap)}
            }
        }
    }
}

def updateQueryViews = {Long projectId, Map fieldOptionNameKeyMap ->
    cardQueryViewQueryService.selectViews(projectId)?.each {view ->
        def detail = cardQueryViewDao.find(view.id)
        if (detail?.queries) {
            detail.queries.each {query -> updateQuery(query, fieldOptionNameKeyMap)}
            cardQueryViewDao.saveOrUpdate(view.id, detail)
        }
    }
}

def updateChart = { chart, Map fieldOptionNameKeyMap ->
    def queries = null
    catchRun {queries = chart['queries']}
    if (queries instanceof Collection) {
        (queries as Collection).each {query -> updateQuery(query, fieldOptionNameKeyMap)}
    }
    def config = null
    catchRun {config = chart['config']}
    if (config instanceof CardsMultiTrend) {
        (config as CardsMultiTrend).getYConfigs()?.each {
            it?.queries?.each {query -> updateQuery(query, fieldOptionNameKeyMap)}
        }
    }
}

def updateProjectCharts = {Long projectId, Map fieldOptionNameKeyMap ->
    chartQueryService.selectByProjectId(projectId)?.each {chart ->
        def detail = chartDao.find(chart.id)
        updateChart(detail, fieldOptionNameKeyMap)
        chartDao.saveOrUpdate(chart.id, detail)
    }
}

def sysField = {
    def schema = schemaHelper.getSysProjectCardSchema()
    schema.fields?.each {field ->
        field.options.eachWithIndex { CardField.Option option, int i ->
            option.key = "option_${i + 1}"
            cardDao.updateByQuery(
                    [
                            Eq.builder().field(field.key).value(option.name).build()
                    ],
                    field.key,
                    option.key
            )
            cardEventDao.updateByQuery(
                    [
                            Eq.builder().field(CardEvent.cardProp(field.key)).value(option.name).build()
                    ],
                    field.key,
                    option.key
            )
        }
    }}

def projectCustomField = {
    def pageNum = 1
    def pageSize = 1000
    def sysFieldOptionNameKeyMap = getFieldOptionNameKeyMap(schemaHelper.getSysProjectCardSchema())
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.each {project ->
            def projectId = project.id
            def schema = schemaDao.find(projectId)
            schema.fields?.each {field ->
                if (field.key.startsWith("custom_") && field.options?.size() > 0) {
                    def nameKey = [:]
                    field.options.eachWithIndex { CardField.Option option, int i ->
                        if (!option.key && !nameKey[option.name]) {
                            option.key = "option_${i + 1}"
                        }
                    }
                }
            }
            schemaDao.saveOrUpdate(projectId, schema)

            def fieldOptionNameKeyMap = getFieldOptionNameKeyMap(schema)
            fieldOptionNameKeyMap.putAll(sysFieldOptionNameKeyMap)

            updateCard(projectId, fieldOptionNameKeyMap)
            updateCardEvent(projectId, fieldOptionNameKeyMap)

            updateFieldFlows(schema.fieldFlows, fieldOptionNameKeyMap)

            updateQueryViews(projectId, fieldOptionNameKeyMap)

            updateProjectCharts(projectId, fieldOptionNameKeyMap)
        }
        pageNum++
    }
}


def projectTemplateField = {
    templateMapper.selectByExample(new ProjectTemplateExample())?.each {
        def templateId = it.id
        def detail = templateDetailDao.find(templateId)
        if (detail?.projectCardSchema) {
            def schema = detail.projectCardSchema
            schema.fields?.each {field ->
                field.options.eachWithIndex { CardField.Option option, int i ->
                    option.key = "option_${i + 1}"
                }
            }
            def fieldOptionNameKeyMap = getFieldOptionNameKeyMap(schema)
            updateFieldFlows(schema.fieldFlows, fieldOptionNameKeyMap)
            templateDetailDao.saveOrUpdate(templateId, detail)
        }
    }
}

def insightCharts = {
    def schema = schemaHelper.getSysProjectCardSchema()
    def fieldOptionNameKeyMap = getFieldOptionNameKeyMap(schema)
    def index = EsIndexUtil.PREFIX + 'ezinsight-chart-config'
    chartDao.scrollAll(index, [], {hit ->
        try {
            def chart = chartDao.JSON_MAPPER.readValue(chartDao.getDocSource(index, hit.id), EzInsightChart.class)
            if (chart) {
                updateChart(chart, fieldOptionNameKeyMap)
                chartDao.setDocSourceJson(index, hit.id, chartDao.JSON_MAPPER.writeValueAsString(chart))
            }
        } catch (Throwable e) {
            // do nothing
        }
    })
}

try {
    projectCustomField()
    projectTemplateField()
    insightCharts()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"