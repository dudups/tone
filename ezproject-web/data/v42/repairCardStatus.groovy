/**
 * 20221210
 * 指定卡片类型状态关闭与打开（asyncMigrateForCloseStatus失败），导致状态变更后的后续计算字段没有更新（changeFields:STATUS、BPM_FLOW_ID、BPM_FLOW_TO_STATUS）
 * 设置项目schema下具体卡片类型的状态列表、包含设置结束状态（asyncChangeStatusIsEnd失败），导致项目卡片类型设置结束或取消时，卡片是否结束错误(changeFields:CALC_IS_END)及状态异常
 * 归档或取消（onPlanActive及onPlanInActive失败），导致计划归档但卡片未归档。changeFields:PLAN_IS_ACTIVE
 */

import com.ezone.ezproject.dal.entity.Card
import com.ezone.ezproject.dal.entity.CardExample
import com.ezone.ezproject.dal.entity.Plan
import com.ezone.ezproject.dal.entity.Project
import com.ezone.ezproject.dal.mapper.CardMapper
import com.ezone.ezproject.es.dao.CardDao
import com.ezone.ezproject.es.dao.CardEventDao
import com.ezone.ezproject.es.dao.ProjectCardSchemaDao
import com.ezone.ezproject.es.entity.CardField
import com.ezone.ezproject.es.entity.CardType
import com.ezone.ezproject.es.entity.ProjectCardSchema
import com.ezone.ezproject.modules.card.bean.SearchEsRequest
import com.ezone.ezproject.modules.card.bean.query.*
import com.ezone.ezproject.modules.card.field.FieldUtil
import com.ezone.ezproject.modules.card.service.CardSearchService
import com.ezone.ezproject.modules.plan.service.PlanQueryService
import com.ezone.ezproject.modules.project.service.ProjectQueryService
import com.ezone.galaxy.framework.common.spring.SpringBeanFactory
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang.exception.ExceptionUtils

import java.util.stream.Collectors

def projectQueryService = SpringBeanFactory.getBean(ProjectQueryService.class)
def schemaDao = SpringBeanFactory.getBean(ProjectCardSchemaDao.class)
def cardSearchService = SpringBeanFactory.getBean(CardSearchService.class)
def planQueryService = SpringBeanFactory.getBean(PlanQueryService.class)
def cardDao = SpringBeanFactory.getBean(CardDao.class)
def cardMapper = SpringBeanFactory.getBean(CardMapper.class)
def eventDao = SpringBeanFactory.getBean(CardEventDao.class)

String[] fields = [CardField.TYPE, CardField.STATUS, CardField.CALC_IS_END, CardField.BPM_FLOW_ID, CardField.PLAN_IS_ACTIVE, CardField.SEQ_NUM, CardField.END_DATE, CardField.LAST_END_TIME, CardField.LAST_END_DELAY]
/**处理卡片计划是否归档属性*/
def processPlanCardActiveError = { Project project ->
//    println("处理项目:[" + project.getName() + "] 活跃计划")
    List<Plan> plans = planQueryService.selectByProjectId(project.getId(), true)
    List<String> activePlanIds = new ArrayList<>()
    if (CollectionUtils.isNotEmpty(plans)) {
        List<String> planIds = plans.stream().map({ plan -> plan.getId().toString() }).collect(Collectors.toList())
        activePlanIds.addAll(planIds)
    }
    activePlanIds.add("0")

    //处理活跃计划及需求池
    def activeQueries = Arrays.asList(In.builder().field(CardField.PLAN_ID).values(activePlanIds).build(),
            Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(project.getId())).build(),
            Eq.builder().field(CardField.PLAN_IS_ACTIVE).value(String.valueOf(false)).build()
    )
    SearchEsRequest activePlan = SearchEsRequest.builder()
            .queries(activeQueries)
            .fields(fields)
            .build()
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        def planErrCards = cardSearchService.search(activePlan, pageNum, pageSize)
        if (CollectionUtils.isEmpty(planErrCards.getList())) {
            break
        }
        List<Long> errCardIds = planErrCards.getList().stream().map({ cardBean -> cardBean.getId() }).collect(Collectors.toList())
        println("处理异常卡片数：" + errCardIds.size())
        List<Query> eventQuery = lastEventQuery(errCardIds, cardMapper)
        cardDao.updateByQuery(activeQueries, CardField.PLAN_IS_ACTIVE, true)
        eventDao.updateByQuery(eventQuery, CardField.PLAN_IS_ACTIVE, String.valueOf(true))
        pageNum++
    }

    //处理归档计划
    def inActiveQueries = Arrays.asList(NotIn.builder().field(CardField.PLAN_ID).values(activePlanIds).build(),
            Eq.builder().field(CardField.PROJECT_ID).value(String.valueOf(project.getId())).build(),
            Eq.builder().field(CardField.PLAN_IS_ACTIVE).value(String.valueOf(true)).build()
    )
    SearchEsRequest inActivePlan = SearchEsRequest.builder()
            .queries(inActiveQueries)
            .fields(fields)
            .build()
    pageNum = 1
    while (true) {
        def inActivePlanErrCards = cardSearchService.search(inActivePlan, pageNum, pageSize)
        if (CollectionUtils.isEmpty(inActivePlanErrCards.getList())) {
            break
        }
        List<Long> errCardIds = inActivePlanErrCards.getList().stream().map({ cardBean -> cardBean.getId() }).collect(Collectors.toList())
        List<Query> eventQuery = lastEventQuery(errCardIds, cardMapper)
        cardDao.updateByQuery(activeQueries, CardField.PLAN_IS_ACTIVE, false)
        eventDao.updateByQuery(eventQuery, CardField.PLAN_IS_ACTIVE, String.valueOf(false))
        pageNum++
    }
}

private static List<Query> lastEventQuery(List<Long> errCardIds, CardMapper cardMapper) {
    CardExample example = new CardExample()
    example.createCriteria().andIdIn(errCardIds)
    List<Card> cards = cardMapper.selectByExample(example)
    List<String> eventIds = cards.stream().map({ card -> card.getLatestEventId().toString() }).collect(Collectors.toList())
    List<Query> eventQuery = Arrays.asList(Ids.builder().ids(eventIds).build())
    eventQuery
}

/**处理卡片状态删除*/
def processStatusDelete = { ProjectCardSchema schema, Project project ->
    Map<String, CardType> typeMap = schema.types.collectEntries { [it.key, it] }
    typeMap.keySet().forEach({ typeKey ->
        CardType cardType = typeMap.get(typeKey)
        def validStatuses = cardType.getStatuses().stream().map({ typeStatus -> typeStatus.getKey() }).collect(Collectors.toList())
        def queries = Arrays.asList(Eq.builder().field(CardField.PROJECT_ID).value(project.getId().toString()).build(),
                Eq.builder().field(CardField.TYPE).value(typeKey).build(),
                NotIn.builder().field(CardField.STATUS).values(validStatuses).build())

        def lastEndStatus = cardType.getStatuses().stream().sorted({ a, b -> -1 }).filter({ cardStatus -> cardStatus.isEnd }).findFirst()
        def lastNotEndStatus = cardType.getStatuses().stream().sorted({ a, b -> -1 }).filter({ cardStatus -> !cardStatus.isEnd }).findFirst()
        if (!lastEndStatus.isPresent()) {
            lastEndStatus = lastNotEndStatus
        }
        if (!lastNotEndStatus.isPresent()) {
            lastNotEndStatus = lastEndStatus
        }

        SearchEsRequest deleteStatus = SearchEsRequest.builder()
                .queries(queries)
                .fields(fields)
                .build()

        def pageNum = 1
        def pageSize = 1000

        while (true) {
            def statusDeleteCards = cardSearchService.search(deleteStatus, pageNum, pageSize)
            pageNum++
            if (CollectionUtils.isEmpty(statusDeleteCards.getList())) {
                break
            }
            List<Long> endCards = new ArrayList<>()
            List<Long> notEndCards = new ArrayList<>()
            List<Long> errCardIds = new ArrayList<>()
            statusDeleteCards.getList().forEach({ cardBean ->
                def cardDetail = cardBean.getCard()
                def cardIsEnd = FieldUtil.getCalcIsEnd(cardDetail)
//                def cardSeqNum = FieldUtil.getSeqNum(cardDetail)
//                println(typeKey + "存在已经删除状态的卡片：" + cardSeqNum)
                if (cardIsEnd) {
                    endCards.add(cardBean.getId())
                } else {
                    notEndCards.add(cardBean.getId())
                }
                errCardIds.add(cardBean.getId())
            })
            if (CollectionUtils.isNotEmpty(notEndCards)) {
                Map<String, Object> notEndProps = new HashMap<>()
                notEndProps.put(CardField.STATUS, lastEndStatus.get().getKey())
                notEndProps.put(CardField.CALC_IS_END, lastNotEndStatus.get().isEnd())
                def ids = notEndCards.stream().map({ id -> String.valueOf(id) }).collect(Collectors.toList())
                List<Query> notEndCardsQueries = Arrays.asList(Ids.builder().ids(ids).build())
                cardDao.updateByQuery(notEndCardsQueries, notEndProps)
                eventDao.updateByQuery(lastEventQuery(notEndCards, cardMapper), CardField.STATUS, lastNotEndStatus.get().getKey())
                eventDao.updateByQuery(lastEventQuery(notEndCards, cardMapper), CardField.CALC_IS_END, String.valueOf(lastNotEndStatus.get().isEnd()))
            }

            if (CollectionUtils.isNotEmpty(endCards)) {
                Map<String, Object> endProps = new HashMap<>()
                endProps.put(CardField.STATUS, lastEndStatus.get().getKey())
                endProps.put(CardField.CALC_IS_END, lastEndStatus.get().isEnd())
                def ids = endCards.stream().map({ id -> String.valueOf(id) }).collect(Collectors.toList())
                List<Query> endCardsQueries = Arrays.asList(Ids.builder().ids(ids).build())
                cardDao.updateByQuery(endCardsQueries, endProps)
                eventDao.updateByQuery(lastEventQuery(endCards, cardMapper), CardField.STATUS, lastEndStatus.get().getKey())
                eventDao.updateByQuery(lastEventQuery(endCards, cardMapper), CardField.CALC_IS_END, String.valueOf(lastEndStatus.get().isEnd()))
            }
        }
    })
}

/**处理结束状态与卡片不一致的*/
def processEndStatusError = { ProjectCardSchema schema, Project project, boolean typeStatusIsEnd ->
    Map<String, CardType> typeMap = schema.types.collectEntries { [it.key, it] }
    typeMap.keySet().forEach({ typeKey ->
        CardType cardType = typeMap.get(typeKey)
        def statusKeys = cardType.getStatuses().stream().filter({ typeStatus -> typeStatus.isEnd == typeStatusIsEnd })
                .map({ typeStatus -> typeStatus.getKey() }).collect(Collectors.toList())
        if (CollectionUtils.isEmpty(statusKeys)) {
            return
        }
        def endQueries = Arrays.asList(Eq.builder().field(CardField.PROJECT_ID).value(project.getId().toString()).build(),
                Eq.builder().field(CardField.TYPE).value(typeKey).build(),
                Eq.builder().field(CardField.CALC_IS_END).value(String.valueOf(!typeStatusIsEnd)).build(),
                In.builder().field(CardField.STATUS).values(statusKeys).build())
        SearchEsRequest endStatusErrorCard = SearchEsRequest.builder()
                .queries(endQueries)
                .fields(fields)
                .build()
        def pageNum = 1
        def pageSize = 1000
        while (true) {
            def isEndErrorCards = cardSearchService.search(endStatusErrorCard, pageNum, pageSize)
            pageNum++
            if (CollectionUtils.isEmpty(isEndErrorCards.getList())) {
                break
            }
            def errIds = isEndErrorCards.getList().stream().map({ cardBean -> cardBean.getId() }).collect(Collectors.toList())
//            def errSeqNum = isEndErrorCards.getList().stream().map({ cardBean -> FieldUtil.getSeqNum(cardBean.getCard()) }).collect(Collectors.toList())
//            println("错误的卡片" + errSeqNum)
            cardDao.updateByQuery(Arrays.asList(Ids.builder().ids(errIds.stream().map({ id -> id.toString() }).collect(Collectors.toList())).build()),
                    CardField.CALC_IS_END, typeStatusIsEnd)
            def eventQueries = lastEventQuery(errIds, cardMapper)
            eventDao.updateByQuery(eventQueries, CardField.CALC_IS_END, String.valueOf(typeStatusIsEnd))
        }
    })
}


def fixProjectCard = {
    def pageNum = 1
    def pageSize = 1000
    while (true) {
        List<Project> projects = projectQueryService.selectAll(pageNum, pageSize)
        if (!projects) {
            break
        }
        projects.each { project ->
            def projectId = project.id
            //测试，去掉  682500376620969984 (test-yonlai111)  438289171731386368 - p10 dev  500852062526103552-internal
//            if (projectId != 438289171731386368) {
//                return
//            }
            def schema = schemaDao.find(projectId)
            if (schema?.types) {
                processPlanCardActiveError(project)
                processStatusDelete(schema, project)
                processEndStatusError(schema, project, true)
                processEndStatusError(schema, project, false)
            }
        }
        pageNum++
    }
}

try {
    fixProjectCard()
} catch (Exception e) {
    println ExceptionUtils.getFullStackTrace(e)
}
println "done"