package com.ezone.ezproject.modules.query.service;

import com.ezone.ezproject.dal.entity.CardQueryView;
import com.ezone.ezproject.dal.entity.CardQueryViewExample;
import com.ezone.ezproject.dal.entity.enums.CardQueryViewType;
import com.ezone.ezproject.dal.mapper.CardQueryViewMapper;
import com.ezone.ezproject.es.dao.CardQueryViewDao;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class CardQueryViewQueryService {
    private CardQueryViewMapper viewMapper;

    private CardQueryViewDao viewDao;

    public CardQueryView select(Long id) {
        return viewMapper.selectByPrimaryKey(id);
    }

    public List<CardQueryView> selectViews(Long projectId) {
        CardQueryViewExample example = new CardQueryViewExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return viewMapper.selectByExample(example);
    }

    public List<CardQueryView> selectShareViews(Long projectId) {
        CardQueryViewExample example = new CardQueryViewExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andTypeEqualTo(CardQueryViewType.SHARE.name());
        return viewMapper.selectByExample(example);
    }

    public List<CardQueryView> selectUserViews(Long projectId, String user) {
        CardQueryViewExample example = new CardQueryViewExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andTypeEqualTo(CardQueryViewType.USER.name()).andCreateUserEqualTo(user);
        return viewMapper.selectByExample(example);
    }

    public List<CardQueryView> selectViews(Long projectId, String user) {
        CardQueryViewExample example = new CardQueryViewExample();
        example.createCriteria().andProjectIdEqualTo(projectId).andTypeEqualTo(CardQueryViewType.USER.name()).andCreateUserEqualTo(user);
        example.or().andProjectIdEqualTo(projectId).andTypeEqualTo(CardQueryViewType.SHARE.name());
        return viewMapper.selectByExample(example);
    }

    public SearchEsRequest selectDetail(Long id) throws IOException {
        return viewDao.find(id);
    }

    @NotNull
    public Long selectShareViewMaxRank(Long projectId) {
        List<CardQueryView> views = selectShareViews(projectId);
        return maxRank(views);
    }

    @NotNull
    public Long selectUserViewMaxRank(Long projectId, String user) {
        List<CardQueryView> views = selectUserViews(projectId, user);
        return maxRank(views);
    }

    @NotNull
    private Long maxRank(List<CardQueryView> views) {
        if (CollectionUtils.isEmpty(views)) {
            return 0L;
        }
        return views.stream().mapToLong(CardQueryView::getRank).max().orElse(0L);
    }

}
