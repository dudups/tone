package com.ezone.ezproject.modules.card.service;

import com.ezone.ezbase.iam.bean.LoginUser;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.function.CacheableFunction;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CardType;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.enums.FieldType;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.field.SingleFieldBatchUpdateHelper;
import com.ezone.ezproject.modules.card.field.check.ParentIdFieldValueChecker;
import com.ezone.ezproject.modules.event.EventDispatcher;
import com.ezone.ezproject.modules.hook.service.WebHookProjectCmdService;
import com.ezone.ezproject.modules.plan.service.PlanQueryService;
import com.ezone.ezproject.modules.project.service.ProjectQueryService;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.ezproject.modules.storymap.service.StoryMapQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author cf
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CardDao.class, CardMapper.class, ProjectCardSchema.class, ParentIdFieldValueChecker.class, ParentIdFieldValueChecker.ParentIdFieldValueCheckerBuilder.class})
class BatchUpdateParentIdHelperTest {

    CardDao cardDao;
    CardMapper cardMapper;
    ProjectCardSchema schema;
    List<Card> cardsInDb;
    Map<Long, Card> cardMap;
    CardQueryService cardQueryService;
    private static final Long PROJECT_ID = 665821422703099904L;

    CardCmdService cardCmdService;
    PlanQueryService planQueryService;
    UserService userService;
    ProjectSchemaQueryService projectSchemaQueryService;
    private StoryMapQueryService storyMapQueryService;
    private EventDispatcher eventDispatcher;
    private WebHookProjectCmdService webHookProjectCmdService;
    private CompanyService companyService;
    private HashMap<Long, Card> allCards = new HashMap<>();
    @Mock
    private ProjectQueryService projectQueryService;

    @BeforeEach
    void before() throws Exception {
        cardDao = PowerMockito.mock(CardDao.class);

        cardQueryService = PowerMockito.mock(CardQueryService.class);
        Mockito.when(cardQueryService.selectDescendant(Mockito.anyLong())).thenCallRealMethod();

        cardsInDb = new ArrayList<>();
        cardMap = new HashMap<>();
        cardMapper = PowerMockito.mock(CardMapper.class);
        Map<Long, Map<String, Object>> allCardMap = new HashMap<>();
        for (long id = 0; id < 100; id++) {
            Card card = Card.builder().id(id)
                    .projectId(PROJECT_ID)
                    .deleted(false)
                    .ancestorId(0L)
                    .seqNum(id)
                    .parentId(0L).build();

            cardMap.put(id, card);
            HashMap<String, Object> cardDetail = new HashMap();
            cardDetail.put(CardField.PARENT_ID, 0);
            cardDetail.put(CardField.PROJECT_ID, PROJECT_ID);
            cardDetail.put(CardField.DELETED, false);
            allCardMap.put(id, cardDetail);
            Mockito.when(cardDao.findAsMap(id)).thenReturn(cardDetail);
            Mockito.when(cardQueryService.select(id)).thenReturn(card);
            Mockito.when(cardMapper.selectByPrimaryKey(id)).thenReturn(card);
            allCards.put(id, card);
        }

        Mockito.when(cardDao.findAsMap(Mockito.anyList())).thenReturn(allCardMap);
        userService = PowerMockito.mock(UserService.class);
        Mockito.when(userService.currentUser()).thenReturn(LoginUser.builder().username("yinchengfeng_dev").build());

        planQueryService = PowerMockito.mock(PlanQueryService.class);
        Mockito.when(planQueryService.select(Mockito.anyLong())).thenReturn(Plan.builder().id(1111L).build());

        schema = PowerMockito.mock(ProjectCardSchema.class);
        CardType cardType = CardType.builder().key("bug").enable(true)
                .field(CardType.FieldConf.builder().enable(true).key(CardField.PARENT_ID).build())
                .field(CardType.FieldConf.builder().enable(true).key(CardField.PROJECT_ID).build())
                .build();
        Mockito.when(schema.findCardType(Mockito.any())).thenReturn(cardType);
        CardField cardField = CardField.builder().key(CardField.PARENT_ID).type(FieldType.LONG).build();
        Mockito.when(schema.findCardField(Mockito.any())).thenReturn(cardField);

        projectSchemaQueryService = PowerMockito.mock(ProjectSchemaQueryService.class);
        ;
        Mockito.when(projectSchemaQueryService.getProjectCardSchema(PROJECT_ID)).thenReturn(schema);

        storyMapQueryService = PowerMockito.mock(StoryMapQueryService.class);

        eventDispatcher = PowerMockito.mock(EventDispatcher.class);
        webHookProjectCmdService = PowerMockito.mock(WebHookProjectCmdService.class);
        companyService = PowerMockito.mock(CompanyService.class);

//        ParentIdFieldValueChecker check = PowerMockito.mock(ParentIdFieldValueChecker.class);
//        PowerMockito.mockStatic(ParentIdFieldValueChecker.class,  Answers.RETURNS_DEEP_STUBS);
//        ParentIdFieldValueChecker singleton = PowerMockito.mock(ParentIdFieldValueChecker.class);
//        PowerMockito.when(ParentIdFieldValueChecker.builder().build()).thenReturn(singleton);
//        ParentIdFieldValueChecker.ParentIdFieldValueCheckerBuilder
//        PowerMockito.whenNew(ParentIdFieldValueChecker.class).withNoArguments().thenReturn(check);
        projectQueryService = PowerMockito.mock(ProjectQueryService.class);
        PowerMockito.when(projectQueryService.select(Mockito.anyLong())).thenReturn(Project.builder().id(PROJECT_ID).build());

    }

    @Test
    void batchUpdateField_error() throws IOException {
        cardsInDb.add(cardMap.get(5L));
        cardsInDb.forEach(System.out::println);
        try {
            System.out.println("这里是分界线-----------------------------------------------------------");
            testBatchUpdateParentIdHelper(CardField.PARENT_ID, "5");
        } catch (CodedException e) {
            e.printStackTrace();
            Assertions.assertTrue( e.getMessage().contains("不能设置自己为父卡片！"));
        }
        cardsInDb.forEach(System.out::println);
    }

    private void testBatchUpdateParentIdHelper(String fieldName, String fieldValue) throws IOException {
        Function<Long, Plan> findPlanById = CacheableFunction.instance(planQueryService::select);
        Function<Long, Card> findCardById = CacheableFunction.instance(cardQueryService::select);
        LoginUser user = userService.currentUser();
        SingleFieldBatchUpdateHelper.builder()
                .user(user.getUsername())
                .schema(projectSchemaQueryService.getProjectCardSchema(PROJECT_ID))
                .cardDao(cardDao)
                .cardMapper(cardMapper)
                .findCardById(findCardById)
                .findCardsDescendant(cardQueryService::selectDescendant)
                .findCardsAncestor(cardQueryService::selectAncestor)
                .findPlanById(findPlanById)
                .findStoryMapNodeById(storyMapQueryService::selectStoryMapNodeById)
                .findStoryMapL2NodeInfoById(storyMapQueryService::selectStoryMapL2NodeInfoById)
                .cards(cardsInDb)
                .cardsJson(cardDao.findAsMap(cardsInDb.stream().map(Card::getId).collect(Collectors.toList())))
                .projectQueryService(projectQueryService)
                .projectId(PROJECT_ID)
                .build()
                .update( fieldName, fieldValue);
    }

    @Test
    void batchUpdateField2_error() throws IOException {
        cardMap.get(2L).setParentId(1L);
        cardMap.get(2L).setAncestorId(1L);
        cardMap.get(3L).setParentId(2L);
        cardMap.get(3L).setAncestorId(1L);
        cardMap.get(4L).setParentId(3L);
        cardMap.get(4L).setAncestorId(1L);
        cardMap.get(5L).setParentId(4L);
        cardMap.get(5L).setAncestorId(1L);
        cardsInDb.add(cardMap.get(1L));
        Map<Long, List<Card>> descendants = new HashMap<>();
        descendants.put(1L, Arrays.asList(cardMap.get(2L), cardMap.get(3L), cardMap.get(4L), cardMap.get(5L)));
        Mockito.when(cardQueryService.selectDescendant(Mockito.anyList())).thenReturn(descendants);

        Map<Long, List<Card>> ancestors = new HashMap<>();
        ancestors.put(4L, Arrays.asList(cardMap.get(1L), cardMap.get(2L), cardMap.get(3L)));
        Mockito.when(cardQueryService.selectAncestor(Mockito.anyList())).thenReturn(ancestors);

        cardsInDb.forEach(System.out::println);
        try {
            System.out.println("输出分界线-----------------------------------------------------------");
            testBatchUpdateParentIdHelper(CardField.PARENT_ID, "4");
        } catch (CodedException e) {
            e.printStackTrace();
            assert e.getMessage() != null;
            Assertions.assertTrue(e.getMessage().contains("存在循环引用"));
        }
        cardsInDb.forEach(System.out::println);

        System.out.println("这里是用例分界线-----------------------------------------------------------");
        ancestors.put(5L, Arrays.asList(cardMap.get(1L), cardMap.get(2L), cardMap.get(3L), cardMap.get(4L)));
        Mockito.when(cardQueryService.selectAncestor(Mockito.anyList())).thenReturn(ancestors);

        cardsInDb.forEach(System.out::println);
        try {
            System.out.println("输出分界线-----------------------------------------------------------");
            testBatchUpdateParentIdHelper(CardField.PARENT_ID, "5");
        } catch (CodedException e) {
            e.printStackTrace();
            assert e.getMessage() != null;
            Assertions.assertTrue(e.getMessage().contains("存在循环引用"));
        }
        cardsInDb.forEach(System.out::println);
    }

    @Test
    void batchUpdateField3_error() throws IOException {
        cardMap.get(2L).setParentId(1L);
        cardMap.get(2L).setAncestorId(1L);
        cardMap.get(3L).setParentId(2L);
        cardMap.get(3L).setAncestorId(1L);
        cardMap.get(4L).setParentId(3L);
        cardMap.get(4L).setAncestorId(1L);
        cardMap.get(5L).setParentId(4L);
        cardMap.get(5L).setAncestorId(1L);
        cardsInDb.add(cardMap.get(10L));
        cardsInDb.add(cardMap.get(1L));
        Map<Long, List<Card>> descendants = new HashMap<>();
        descendants.put(1L, Arrays.asList(cardMap.get(2L), cardMap.get(3L), cardMap.get(4L), cardMap.get(5L)));
        descendants.put(10L, Collections.emptyList());
        Mockito.when(cardQueryService.selectDescendant(Mockito.anyList())).thenReturn(descendants);

        Map<Long, List<Card>> ancestors = new HashMap<>();
        ancestors.put(5L, Arrays.asList(cardMap.get(1L), cardMap.get(2L), cardMap.get(3L), cardMap.get(4L)));
        Mockito.when(cardQueryService.selectAncestor(Mockito.anyList())).thenReturn(ancestors);

        cardsInDb.forEach(System.out::println);
        try {
            System.out.println("输出分界线-----------------------------------------------------------");
            testBatchUpdateParentIdHelper(CardField.PARENT_ID, "5");
        } catch (CodedException e) {
            e.printStackTrace();
            assert e.getMessage() != null;
            Assertions.assertTrue(e.getMessage().contains("存在循环引用"));
        }
        cardsInDb.forEach(System.out::println);

        System.out.println("这里是用例分界线-----------------------------------------------------------");
    }

    @Test
    void batchUpdateField4_success() throws IOException {
        cardMap.get(2L).setParentId(1L);
        cardMap.get(2L).setAncestorId(1L);
        cardMap.get(3L).setParentId(2L);
        cardMap.get(3L).setAncestorId(1L);
        cardMap.get(4L).setParentId(3L);
        cardMap.get(4L).setAncestorId(1L);
        cardMap.get(5L).setParentId(4L);
        cardMap.get(5L).setAncestorId(1L);
        cardsInDb.add(cardMap.get(3L));
        cardsInDb.add(cardMap.get(5L));
        Map<Long, List<Card>> descendants = new HashMap<>();
        descendants.put(1L, Arrays.asList(cardMap.get(2L), cardMap.get(3L), cardMap.get(4L), cardMap.get(5L)));
        descendants.put(3L, Arrays.asList(cardMap.get(4L), cardMap.get(5L)));
        descendants.put(5L, Collections.emptyList());
        Mockito.when(cardQueryService.selectDescendant(Mockito.anyList())).thenReturn(descendants);

        Map<Long, List<Card>> ancestors = new HashMap<>();
        ancestors.put(5L, Arrays.asList(cardMap.get(1L), cardMap.get(2L), cardMap.get(3L), cardMap.get(4L)));
        ancestors.put(0L, Collections.emptyList());
        Mockito.when(cardQueryService.selectAncestor(Mockito.anyList())).thenReturn(ancestors);

        Assertions.assertEquals(2L, (long) cardMap.get(3L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(3L).getAncestorId());

        Assertions.assertEquals(3L, (long) cardMap.get(4L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(4L).getAncestorId());

        Assertions.assertEquals(4L, (long) cardMap.get(5L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(5L).getAncestorId());

//        cardsInDb.forEach(System.out::println);
        System.out.println("输出分界线-----------------------------------------------------------");
        testBatchUpdateParentIdHelper(CardField.PARENT_ID, "0");

        Assertions.assertEquals(0L, (long) cardMap.get(3L).getParentId());
        Assertions.assertEquals(0L, (long) cardMap.get(3L).getAncestorId());

        Assertions.assertEquals(3L, (long) cardMap.get(4L).getParentId());
        Assertions.assertEquals(3L, (long) cardMap.get(4L).getAncestorId());

        Assertions.assertEquals(0L, (long) cardMap.get(5L).getParentId());
        Assertions.assertEquals(0L, (long) cardMap.get(5L).getAncestorId());

        System.out.println("这里是用例分界线-----------------------------------------------------------");
    }

    @Test
    void batchUpdateField5_success() throws IOException {
        cardMap.get(2L).setParentId(1L);
        cardMap.get(2L).setAncestorId(1L);
        cardMap.get(3L).setParentId(2L);
        cardMap.get(3L).setAncestorId(1L);
        cardMap.get(4L).setParentId(3L);
        cardMap.get(4L).setAncestorId(1L);
        cardMap.get(5L).setParentId(4L);
        cardMap.get(5L).setAncestorId(1L);
        cardsInDb.add(cardMap.get(3L));
        cardsInDb.add(cardMap.get(4L));
        Map<Long, List<Card>> descendants = new HashMap<>();
        descendants.put(1L, Arrays.asList(cardMap.get(2L), cardMap.get(3L), cardMap.get(4L), cardMap.get(5L)));
        descendants.put(3L, Arrays.asList(cardMap.get(4L), cardMap.get(5L)));
        descendants.put(5L, Collections.emptyList());
        Mockito.when(cardQueryService.selectDescendant(Mockito.anyList())).thenReturn(descendants);

        Map<Long, List<Card>> ancestors = new HashMap<>();
        ancestors.put(5L, Arrays.asList(cardMap.get(1L), cardMap.get(2L), cardMap.get(3L), cardMap.get(4L)));
        ancestors.put(0L, Collections.emptyList());
        Mockito.when(cardQueryService.selectAncestor(Mockito.anyList())).thenReturn(ancestors);

        Assertions.assertEquals(2L, (long) cardMap.get(3L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(3L).getAncestorId());

        Assertions.assertEquals(3L, (long) cardMap.get(4L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(4L).getAncestorId());

        Assertions.assertEquals(4L, (long) cardMap.get(5L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(5L).getAncestorId());

//        System.out.println("输出分界线-----------------------------------------------------------");

        testBatchUpdateParentIdHelper(CardField.PARENT_ID, "7");

        Assertions.assertEquals(7L, (long) cardMap.get(3L).getParentId());
        Assertions.assertEquals(7L, (long) cardMap.get(3L).getAncestorId());

        Assertions.assertEquals(7L, (long) cardMap.get(4L).getParentId());
        Assertions.assertEquals(7L, (long) cardMap.get(4L).getAncestorId());

        Assertions.assertEquals(4L, (long) cardMap.get(5L).getParentId());
        Assertions.assertEquals(7L, cardMap.get(5L).getAncestorId().longValue());

        System.out.println("这里是用例分界线-----------------------------------------------------------");
    }


    @Test
    void batchUpdateField6_success() throws IOException {
        cardMap.get(2L).setParentId(1L);
        cardMap.get(2L).setAncestorId(1L);
        cardMap.get(3L).setParentId(1L);
        cardMap.get(3L).setAncestorId(1L);
        cardMap.get(4L).setParentId(1L);
        cardMap.get(4L).setAncestorId(1L);
        cardMap.get(5L).setParentId(4L);
        cardMap.get(5L).setAncestorId(1L);

        cardsInDb.add(cardMap.get(4L));
        Map<Long, List<Card>> descendants = new HashMap<>();
        descendants.put(1L, Arrays.asList(cardMap.get(2L), cardMap.get(3L), cardMap.get(4L), cardMap.get(5L)));
        descendants.put(2L, Collections.emptyList());
        descendants.put(3L, Collections.emptyList());
        descendants.put(4L, Collections.singletonList(cardMap.get(5L)));
        descendants.put(5L, Collections.emptyList());
        Mockito.when(cardQueryService.selectDescendant(Mockito.anyList())).thenReturn(descendants);

        Map<Long, List<Card>> ancestors = new HashMap<>();
        ancestors.put(3L, Collections.singletonList(cardMap.get(1L)));
        ancestors.put(4L, Collections.singletonList(cardMap.get(1L)));
        ancestors.put(5L, Arrays.asList(cardMap.get(4L),cardMap.get(1L)));
        Mockito.when(cardQueryService.selectAncestor(Mockito.anyList())).thenReturn(ancestors);

//        System.out.println("输出分界线-----------------------------------------------------------");
        testBatchUpdateParentIdHelper(CardField.PARENT_ID, "3");
        Assertions.assertEquals(1L, (long) cardMap.get(3L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(3L).getAncestorId());

        Assertions.assertEquals(3L, (long) cardMap.get(4L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(4L).getAncestorId());

        Assertions.assertEquals(4L, (long) cardMap.get(5L).getParentId());
        Assertions.assertEquals(1L, (long) cardMap.get(5L).getAncestorId());

        System.out.println("这里是用例分界线-----------------------------------------------------------");
    }
}