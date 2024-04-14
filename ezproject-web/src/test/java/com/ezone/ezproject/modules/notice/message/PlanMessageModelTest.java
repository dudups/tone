package com.ezone.ezproject.modules.notice.message;

import com.ezone.ezproject.common.JsonUtil;
import com.ezone.ezproject.dal.entity.Plan;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.es.entity.ProjectNoticeConfig;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.common.EndpointHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EndpointHelper.class, UserService.class})
class PlanMessageModelTest {

    @Mock
    private EndpointHelper endpointHelper;
    @Mock
    private UserService userService;
    private String companyName = "企业名称";
    private Long companyId = 1L;
    private Project project;

    @BeforeEach
    public void before() throws Exception {
        endpointHelper = PowerMockito.mock(EndpointHelper.class);
        userService = PowerMockito.mock((UserService.class));
        Mockito.when(endpointHelper.planDetailPath(Mockito.anyString(), Mockito.anyLong())).thenReturn("http://localhost/plan/222");
        Mockito.when(endpointHelper.planDetailUrl(Mockito.anyLong(), Mockito.any(), Mockito.anyLong())).thenReturn("http://localhost/plan/222");
        project = Project.builder().id(1L).key("myProject").companyId(companyId).name("我的项目").build();
        Mockito.when(endpointHelper.userHomeUrl(Mockito.anyLong(), Mockito.any())).thenReturn("http://localhost/user/zhangsan");
    }

    @Test
    void getEscapeTitle() {
    }

    @Test
    void getFeiShuContent() {
        PlanMessageModel model = create();
        String feiShuContent = model.getFeiShuContent();
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

    PlanMessageModel create() {
        Plan plan = Plan.builder().name("我的计划").id(1L).build();
        return PlanMessageModel.builder()
                .plan(plan)
                .endpointHelper(endpointHelper)
                .project(project)
                .sender("zhangsan")
                .nickName("张三")
                .operationType(ProjectNoticeConfig.Type.CREATE)
                .build();
    }
}