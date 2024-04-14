import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.dao.CardQueryViewDao
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao
import com.ezone.ezproject.es.entity.enums.FieldType
import com.ezone.ezproject.modules.card.event.model.CardEvent
import com.ezone.ezproject.modules.card.event.model.EventType
import com.ezone.ezproject.modules.card.event.model.UpdateEventMsg
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.query.service.CardQueryViewQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.collections4.ListUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.exception.ExceptionUtils
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)
def schemaHelper = SpringBeanFactory.getBean(ProjectCardSchemaHelper.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)
def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)

def cardQueryViewQueryService= SpringBeanFactory.getBean(CardQueryViewQueryService.class)
def cardQueryViewDao = SpringBeanFactory.getBean(CardQueryViewDao.class)

def projectCustomField = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.each {project ->
            def projectId = project.id
            def schema = schemaDao.find(projectId)
            Map<String, Map<String, String>> fieldOptionNameKey = schema.fields
                    ?.findAll {it.type == FieldType.CHECK_BOX && it?.options?.size() > 0}
                    .collectEntries { field -> [
                            field.key,
                            field.options.collectEntries {option -> [option.name, option.key]}
                    ]}
            if (!fieldOptionNameKey?.size()) {
                return
            }
            ListUtils.partition(cardQueryService.selectByProjectId(projectId), 100).each {cards ->
                BoolQueryBuilder bool = QueryBuilders.boolQuery()
                        .filter(QueryBuilders.termsQuery(CardEvent.CARD_ID, cards.collect {it.id}))
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(bool)
                cardEventDao.searchAll(searchSourceBuilder, 10000).list
                        .groupBy {it.cardId}
                        .each {cardId, events ->
                            if (events?.size()) {
                                def cardProps = [:]
                                def eventProps = [cardDetail: cardProps]
                                events.sort {it.id}.each {event ->
                                    if (event.eventType == EventType.CREATE) {
                                        // nothing
                                    } else if (event.eventType == EventType.UPDATE) {
                                        if (event.eventMsg instanceof UpdateEventMsg) {
                                            def updateEvent = event.eventMsg as UpdateEventMsg
                                            updateEvent.fieldDetailMsgs
                                                    .findAll{fieldOptionNameKey.containsKey(it.fieldKey)}
                                                    .each {msg ->
                                                        def optionNameKey = fieldOptionNameKey[msg.fieldKey]
                                                        cardProps[msg.fieldKey] = StringUtils
                                                                .split(msg.toMsg, msg.toMsg?.contains(" ") ? " " : ",")
                                                                .collect {name ->
                                                                    def key = optionNameKey[name]
                                                                    if (!key) {
                                                                        key = name
                                                                    }
                                                                    key
                                                                }
                                                    }

                                        }
                                        if (cardProps.size()) {
                                            cardEventDao.updateSelective(event.id, eventProps)
                                        }
                                    }
                                }
                                if (cardProps.size()) {
                                    cardDao.updateSelective(cardId, cardProps)
                                    println cardId
                                }
                            }
                        }
            }
        }
        pageNum++
    }
}

try {
    projectCustomField()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"