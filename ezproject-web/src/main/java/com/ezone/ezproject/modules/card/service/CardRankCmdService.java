package com.ezone.ezproject.modules.card.service;

import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.mapper.CardMapper;
import com.ezone.ezproject.es.dao.CardDao;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.rank.RankLocation;
import com.ezone.ezproject.modules.permission.PermissionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@AllArgsConstructor
public class CardRankCmdService {

    private CardMapper cardMapper;

    private CardDao cardDao;

    private CardQueryService cardQueryService;

    private PermissionService permissionService;

    private UserService userService;

    private CardHelper cardHelper;

    /**
     * 卡片重排序；因需查询卡片/计划排序位置的前后相邻记录，故事务隔离级要求可重复读
     * @param projectId
     * @param ids 倒序，会按ids顺序从大到小分配新序号
     * @param referenceRank
     * @param location
     * @return
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public Map<Long, String> rank(Long projectId, List<Long> ids, String referenceRank, RankLocation location) {
        List<String> ranks = cardHelper.ranks(projectId, referenceRank, location, ids.size());
        Map<Long, String> rankMap = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            String rank = ranks.get(ids.size() -1 -i);
            cardMapper.updateByPrimaryKeySelective(Card.builder().id(id).rank(rank).build());
            rankMap.put(ids.get(i), rank);
        }
        return rankMap;
    }

}
