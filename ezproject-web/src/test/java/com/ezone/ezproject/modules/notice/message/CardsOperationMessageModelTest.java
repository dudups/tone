package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.JsonUtil;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.CardField;
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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(EndpointHelper.class)
class CardsOperationMessageModelTest {
    @Mock
    private EndpointHelper endpointHelper;
    private String companyName = "企业名称";
    private Long companyId = 1L;
    private Project project;

    @BeforeEach
    public void before() throws Exception {
        endpointHelper = PowerMockito.mock(EndpointHelper.class);
        Mockito.when(endpointHelper.cardDetailUrl(Mockito.anyLong(), Mockito.any(), Mockito.anyLong())).thenReturn("http://localhost/card/222");
        project = Project.builder().id(1L).key("myProject").companyId(companyId).name("我的项目").build();
        Mockito.when(endpointHelper.userHomeUrl(Mockito.anyLong(), Mockito.any())).thenReturn("http://localhost/user/zhangsan");
    }

    @Test
    void getFeiShuContent() {
        Map<Long, Map<String, Object>> cardDetails = new HashMap<>();
        Map<String, Object> cardDetail = new HashMap<>();
        cardDetails.put(1L, cardDetail);
        cardDetail.put(CardField.PROJECT_ID, 222L);
        cardDetail.put(CardField.TITLE, "卡片标题");
        cardDetail.put(CardField.SEQ_NUM, 1L);
        cardDetail.put(CardField.COMPANY_ID, companyId);
        String sender = "张三";
        CardsOperationMessageModel messageModel = CardsOperationMessageModel.builder()
                .cardDetails(cardDetails)
                .endpointHelper(endpointHelper)
                .project(project)
                .sender(sender)
                .operationType(ProjectNoticeConfig.Type.DELETE)
                .build();
        String feiShuContent = messageModel.getFeiShuContent();
        System.out.println(feiShuContent);

        boolean isJson = JsonUtil.isValidJSON(feiShuContent);
        assertTrue(isJson);
    }

    @Test
    void getContent() {
    }

    @Test
    void getEmailContent() {
    }

}