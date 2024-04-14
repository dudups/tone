package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.JsonUtil;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.CompanyCardSchema;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
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
class CardOperationMessageModelTest {
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
    private static final String cardTypeName = "故事";
    private static final String memberKey = "qa_owner_users";

    @BeforeEach
    public void before() throws Exception {
        endpointHelper = PowerMockito.mock(EndpointHelper.class);
        companyCardSchema = PowerMockito.mock(CompanyCardSchema.class);
        projectCardSchema = PowerMockito.mock(ProjectCardSchema.class);
        Mockito.when(endpointHelper.cardDetailUrl(Mockito.anyLong(), Mockito.any(), Mockito.anyLong())).thenReturn("http://localhost/card/222");
        project = Project.builder().id(1L).key(projectKey).companyId(companyId).name("我的项目").build();
        Mockito.when(endpointHelper.userHomeUrl(Mockito.anyLong(), Mockito.any())).thenReturn("http://localhost/user/zhangsan");
        Mockito.when(companyCardSchema.findCardTypeName(Mockito.any())).thenReturn(cardTypeName);

        Map<String, String> keyNames = new HashMap<>();
        keyNames.put(memberKey, "测试负责人");
        Mockito.when(projectCardSchema.fieldKeyNames(Mockito.any())).thenReturn(keyNames);
    }

    @Test
    void getFeiShuContent() {
        CardOperationMessageModel messageModel = createModel();
        String feiShuContent = messageModel.getFeiShuContent();
        System.out.println(feiShuContent);
        assertTrue(JsonUtil.isValidJSON(feiShuContent));
    }

    @Test
    void getContent() {
    }

    @Test
    void getEmailContent() {
        CardOperationMessageModel messageModel = createModel();
        String emailContent = messageModel.getEmailContent();
        System.out.println(emailContent);
    }


    private CardOperationMessageModel createModel() {
        List<String> memberKeys = Arrays.asList("qa_owner_users");
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
        CardOperationMessageModel messageModel = CardOperationMessageModel.builder()
                .cardDetail(cardDetail)
                .endpointHelper(endpointHelper)
                .companyCardSchema(companyCardSchema)
                .projectCardSchema(projectCardSchema)
                .cardUserNickNames(cardUserNickNames)
                .project(project)
                .sender(sender)
                .nickName(nickname)
                .build();
        return messageModel;
    }
}