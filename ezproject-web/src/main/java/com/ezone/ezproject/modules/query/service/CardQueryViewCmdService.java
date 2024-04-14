package com.ezone.ezproject.modules.query.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.dal.entity.CardQueryView;
import com.ezone.ezproject.dal.entity.enums.CardQueryViewType;
import com.ezone.ezproject.dal.mapper.CardQueryViewMapper;
import com.ezone.ezproject.es.dao.CardQueryViewDao;
import com.ezone.ezproject.es.entity.CardField;
import com.ezone.ezproject.es.entity.ProjectCardSchema;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.bean.query.In;
import com.ezone.ezproject.modules.card.bean.query.Query;
import com.ezone.ezproject.modules.project.service.ProjectCardSchemaHelper;
import com.ezone.ezproject.modules.project.service.ProjectSchemaQueryService;
import com.ezone.ezproject.modules.query.bean.CopyCardQueryViewRequest;
import com.ezone.ezproject.modules.query.bean.CreateCardQueryViewRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class CardQueryViewCmdService {
    private UserService userService;

    private CardQueryViewMapper viewMapper;

    private CardQueryViewDao viewDao;

    private CardQueryViewQueryService viewQueryService;

    private ProjectSchemaQueryService schemaQueryService;

    private CardQueryViewHelper viewHelper;

    private ProjectCardSchemaHelper projectCardSchemaHelper;


    public CardQueryView create(CreateCardQueryViewRequest request) throws IOException {
        String user = userService.currentUserName();


        CardQueryView view = CardQueryView.builder()
                .id(IdUtil.generateId())
                .projectId(request.getProjectId())
                .name(request.getName())
                .type(request.getType().name())
                .rank(0L)
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
        viewMapper.insert(view);
        SearchEsRequest searchCardRequest = request.getRequest();
        setFieldId(view.getProjectId(), searchCardRequest);
        viewDao.saveOrUpdate(view.getId(), searchCardRequest);
        return view;
    }

    public List<CardQueryView> initUserCardQueryViews(Long projectId, String user) throws IOException {
        return Arrays.asList(
                initUserCardQueryView(
                        projectId, user, "全部"
                ),
                initUserCardQueryView(
                        projectId, user, "我创建的",
                        In.builder().field(CardField.CREATE_USER).values(Arrays.asList(user)).build()
                ),
                initUserCardQueryView(
                        projectId, user, "我负责的",
                        In.builder().field(CardField.OWNER_USERS).values(Arrays.asList(user)).build()
                )
        );
    }

    private CardQueryView initUserCardQueryView(Long projectId, String user, String viewName, Query... queries) throws IOException {

        CardQueryView view = CardQueryView.builder()
                .id(IdUtil.generateId())
                .projectId(projectId)
                .name(viewName)
                .type(CardQueryViewType.USER.name())
                .rank(0L)
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
        viewMapper.insert(view);
        SearchEsRequest searchCardRequest = viewHelper.initCardQueryView(queries);
        setFieldId(view.getProjectId(), searchCardRequest);
        viewDao.saveOrUpdate(view.getId(), searchCardRequest);
        return view;
    }

    public CardQueryView copy(CopyCardQueryViewRequest request, CardQueryView copyFrom) throws IOException {
        String user = userService.currentUserName();
        CardQueryView view = CardQueryView.builder()
                .id(IdUtil.generateId())
                .projectId(copyFrom.getProjectId())
                .name(request.getName())
                .type(request.getType().name())
                .rank(0L)
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
        viewMapper.insert(view);
        SearchEsRequest sourceSearchCardRequest = viewDao.find(request.getCopyFromId());
        setFieldId(view.getProjectId(), sourceSearchCardRequest);
        viewDao.saveOrUpdate(view.getId(), sourceSearchCardRequest);
        return view;
    }

    public CardQueryView rename(CardQueryView view, String name) throws IOException {
        String user = userService.currentUserName();
        view.setName(name);
        view.setLastModifyUser(user);
        view.setLastModifyTime(new Date());
        viewMapper.updateByPrimaryKey(view);
        return view;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CardQueryView top(CardQueryView view) {
        String user = userService.currentUserName();
        if (CardQueryViewType.SHARE.equals(view)) {
            view.setRank(1 + viewQueryService.selectShareViewMaxRank(view.getProjectId()));
        } else {
            view.setRank(1 + viewQueryService.selectUserViewMaxRank(view.getProjectId(), user));
        }
        view.setLastModifyUser(user);
        view.setLastModifyTime(new Date());
        viewMapper.updateByPrimaryKey(view);
        return view;
    }

    public CardQueryView update(CardQueryView view, SearchEsRequest request) throws IOException {
        setFieldId(view.getProjectId(), request);
        String user = userService.currentUserName();
        view.setLastModifyUser(user);
        view.setLastModifyTime(new Date());
        viewMapper.updateByPrimaryKey(view);
        viewDao.saveOrUpdate(view.getId(), request);
        return view;
    }

    public void delete(Long id) throws IOException {
        viewMapper.deleteByPrimaryKey(id);
        viewDao.delete(id);
    }

    public void deleteByProject(Long projectId) throws IOException {
        List<CardQueryView> views = viewQueryService.selectViews(projectId);
        if (CollectionUtils.isEmpty(views)) {
            return;
        }
        List<Long> viewIds = new ArrayList<>();
        views.forEach(view -> {
            viewMapper.deleteByPrimaryKey(view.getId());
            viewIds.add(view.getId());
        });
        viewDao.delete(viewIds);
    }

    private void setFieldId(Long projectId, SearchEsRequest request) {
        ProjectCardSchema schema = schemaQueryService.getProjectCardSchema(projectId);
        Map<String, Long> fieldKeyIds = projectCardSchemaHelper.extractCustomFieldId(schema, request.getQueries());
        request.setFieldIds(fieldKeyIds);
    }
}
