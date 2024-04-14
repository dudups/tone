/**
 * 20220429:
 * 添加内置类型
 */


import com.ezone.ezproject.dal.entity.ProjectExample
import com.ezone.ezproject.dal.mapper.ProjectMapper
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.entity.ProjectCardSchema
import com.ezone.ezproject.es.entity.enums.InnerCardType
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.card.service.CardCmdService
import com.ezone.ezproject.modules.card.service.CardQueryService
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.collections4.ListUtils
import org.apache.commons.lang.exception.ExceptionUtils

//process card
def projectMapper = SpringBeanFactory.getBean(ProjectMapper.class)
def cardCmdService = SpringBeanFactory.getBean(CardCmdService.class)
def  projectSchemaQueryService = SpringBeanFactory.getBean(ProjectSchemaQueryService.class)
def setInnerType4Card = {
    ProjectExample example = new ProjectExample()
    def projects = projectMapper.selectByExample(example)
    projects.forEach({ project ->
        if(project!=null){
            def projectId = project.getId()
            projectSchemaQueryService.cleanProjectCardSchemaCache(projectId);
            ProjectCardSchema schema = projectSchemaQueryService.getProjectCardSchema(projectId);
            if (schema != null) {
                schema.getTypes().stream().forEach({cardType ->
                    try {
                        cardCmdService.updateCardInnerType(projectId, cardType.getKey(), cardType.getInnerType());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    })
}


//process card event
def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def cardQueryService = SpringBeanFactory.getBean(CardQueryService.class)
def cardEventDao = SpringBeanFactory.getBean(CardEventDao.class)

def types = new HashMap<String, String>()
types.put("epic", InnerCardType.requirement.name())
types.put("feature", InnerCardType.requirement.name())
types.put("story", InnerCardType.requirement.name())
types.put("bug", InnerCardType.bug.name())
types.put("transaction", InnerCardType.transaction.name())
def setInnerType4Event = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.forEach {
            def projectId = it.id
            def plans = planQueryService.selectByProjectId(projectId, null)
            if (!plans) {
                return
            }
            plans.forEach { plan ->
                def cards = cardQueryService.selectByPlanId(plan.id, false)
                if (!cards) {
                    return
                }
                ListUtils.partition(cards, 200).stream().forEach({ subCards ->
                    def cardEvents = cardEventDao.searchEventIdAndType(subCards.collect { it.getId() })
                    def typeIdsMap = new HashMap<String, List<Long>>()
                    cardEvents.forEach({ event ->
                        def eventIds = typeIdsMap.get(FieldUtil.getType(event.cardDetail), new ArrayList())
                        eventIds.add(event.getId())
                        typeIdsMap.put(FieldUtil.getType(event.cardDetail), eventIds)
                    })

                    typeIdsMap.forEach({ cardType, eventIds ->
                        cardEventDao.setInnerType(eventIds, types.get(cardType))
                    })
                })
            }
        }
        pageNum++
    }
}
try {
    setInnerType4Card()
    setInnerType4Event()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"