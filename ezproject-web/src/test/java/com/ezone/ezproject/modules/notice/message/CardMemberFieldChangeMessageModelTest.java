package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.JsonUtil;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.modules.common.EndpointHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EndpointHelper.class, CompanyCardSchema.class, ProjectCardSchema.class})
class CardMemberFieldChangeMessageModelTest {
    @Mock
    private EndpointHelper endpointHelper;
    @Mock
    private CompanyCardSchema companyCardSchema;
    @Mock
    private ProjectCardSchema projectCardSchema;
    private Project project;

    private static final Long companyId = 1L;
    private static final String companyName = "企业名称";
    private static final String projectKey = "myProject";
    private static final String cardType = "story";

    @BeforeEach
    public void before() throws Exception {
        endpointHelper = PowerMockito.mock(EndpointHelper.class);
        companyCardSchema = PowerMockito.mock(CompanyCardSchema.class);
        projectCardSchema = PowerMockito.mock(ProjectCardSchema.class);
        Mockito.when(endpointHelper.cardDetailUrl(Mockito.anyLong(), Mockito.any(), Mockito.anyLong())).thenReturn("http://localhost/card/222");
        project = Project.builder().id(1L).key(projectKey).companyId(companyId).name("我的项目").build();
        Mockito.when(endpointHelper.userHomeUrl(Mockito.anyLong(), Mockito.any())).thenReturn("http://localhost/user/zhangsan");
        Mockito.when(companyCardSchema.findCardTypeName(Mockito.any())).thenReturn(cardType);

        Map<String, String> keyNames = new HashMap<>();
        keyNames.put(CardField.QA_OWNER_USERS, "测试负责人");
        keyNames.put(CardField.OWNER_USERS, "负责人");
        Mockito.when(projectCardSchema.fieldKeyNames(Mockito.any())).thenReturn(keyNames);
    }

    @Test
    void getFeiShuContent() {
        CardMemberFieldChangeMessageModel messageModel = createModel(CardMemberFieldChangeMessageModel.MemberOperatedType.delete);
        String feiShuContent = messageModel.getFeiShuContent();
        System.out.println(feiShuContent);
        
        assertTrue(JsonUtil.isValidJSON(feiShuContent));
        String escapeTitle = messageModel.getEscapeTitle();
    }

    @Test
    void getFeiShuContent2() {
        CardMemberFieldChangeMessageModel messageModel = createModel(CardMemberFieldChangeMessageModel.MemberOperatedType.add);
        String feiShuContent = messageModel.getFeiShuContent();
        System.out.println(feiShuContent);
        assertTrue(JsonUtil.isValidJSON(feiShuContent));
    }

    @Test
    void getEmailContent() {
        CardMemberFieldChangeMessageModel messageModel = createModel(CardMemberFieldChangeMessageModel.MemberOperatedType.add);
        String emailContent = messageModel.getEmailContent();
        System.out.println(emailContent);
    }

    @Test
    void getEmailContent2() {
        CardMemberFieldChangeMessageModel messageModel = createModel(CardMemberFieldChangeMessageModel.MemberOperatedType.delete);
        String emailContent = messageModel.getEmailContent();
        System.out.println(emailContent);
    }

    private CardMemberFieldChangeMessageModel createModel(CardMemberFieldChangeMessageModel.MemberOperatedType memberOperatedType) {
        List<String> memberKeys = Arrays.asList(CardField.OWNER_USERS);
        Map<String, Object> cardDetail = new HashMap<>();
        cardDetail.put(CardField.PROJECT_ID, 222L);
        String cardTitle = "这里是卡片标题";
        cardDetail.put(CardField.TITLE, cardTitle);
        long seqNum = 1L;
        cardDetail.put(CardField.SEQ_NUM, seqNum);
        cardDetail.put(CardField.COMPANY_ID, companyId);
        String sender = "zhangSan";
        String nickname = "张三";
        List<String> owner = Arrays.asList("李四", "王五");
        cardDetail.put(CardField.CREATE_TIME, new Date().getTime());
        cardDetail.put(CardField.OWNER_USERS, owner);
        cardDetail.put(CardField.QA_OWNER_USERS, "赵六");
        cardDetail.put(CardField.PRIORITY, "P0");
        Map<String, String> cardUserNickNames = new HashMap<>();
        cardUserNickNames.put("yinchengfeng_dev", "尹成丰");
        cardUserNickNames.put("yincf2", "尹成丰备用");
        CardMemberFieldChangeMessageModel messageModel = CardMemberFieldChangeMessageModel.builder()
                .cardDetail(cardDetail)
                .endpointHelper(endpointHelper)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(projectCardSchema)
                .project(project)
                .sender(sender)
                .cardUserNickNames(cardUserNickNames)
                .cardOperatedType(ProjectNoticeConfig.Type.UPDATE)
                .nickName(nickname)
                .changedMemberFieldKeys(memberKeys)
                .memberOperatedType(memberOperatedType)
                .build();
        return messageModel;
    }

    @Test
    void getContent() {
    }


}