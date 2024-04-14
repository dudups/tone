package com.ezone.ezproject.modules.storymap.service;

import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapExample;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.entity.StoryMapNodeExample;
import com.ezone.ezproject.dal.mapper.StoryMapMapper;
import com.ezone.ezproject.dal.mapper.StoryMapNodeMapper;
import com.ezone.ezproject.es.dao.StoryMapQueryDao;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class StoryMapQueryService {
    private StoryMapMapper storyMapMapper;
    private StoryMapNodeMapper storyMapNodeMapper;
    private StoryMapQueryDao storyMapQueryDao;

    private UserService userService;

    public StoryMapNode selectStoryMapNodeById(Long id) {
        return storyMapNodeMapper.selectByPrimaryKey(id);
    }

    public List<StoryMapNode> selectStoryMapNodeByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return ListUtils.EMPTY_LIST;
        }
        StoryMapNodeExample example = new StoryMapNodeExample();
        example.createCriteria().andIdIn(ids);
        return storyMapNodeMapper.selectByExample(example);
    }

    public StoryMap selectStoryMapById(Long id) {
        return storyMapMapper.selectByPrimaryKey(id);
    }

    public SearchEsRequest selectStoryMapQuery(Long storyMapId) throws IOException {
        return storyMapQueryDao.find(storyMapId);
    }

    public String selectStoryMapL2NodeInfoById(Long id) {
        StoryMapNode storyMapNode = storyMapNodeMapper.selectByPrimaryKey(id);
        if (null == storyMapNode || storyMapNode.getParentId().equals(0L)) {
            return null;
        }
        StoryMapNode l1Node = storyMapNodeMapper.selectByPrimaryKey(storyMapNode.getParentId());
        StoryMap storyMap = storyMapMapper.selectByPrimaryKey(storyMapNode.getStoryMapId());
        return StringUtils.joinWith("/", storyMap.getName(), l1Node.getName(), storyMapNode.getName());
    }

    public Map<Long, String> selectStoryMapL2NodeInfoByStoryMapId(Long storyMapId) {
        StoryMap storyMap = storyMapMapper.selectByPrimaryKey(storyMapId);
        if (null == storyMap) {
            return MapUtils.EMPTY_MAP;
        }
        List<StoryMapNode> nodes = selectNodeByStoryMapId(storyMapId);
        if (CollectionUtils.isEmpty(nodes)) {
            return MapUtils.EMPTY_MAP;
        }
        List<StoryMapNode> l2Nodes = nodes.stream().filter(n -> n.getParentId() > 0).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(l2Nodes)) {
            return MapUtils.EMPTY_MAP;
        }
        return l2Nodes.stream().collect(Collectors.toMap(n -> n.getId(), n -> {
            StoryMapNode l1Node = nodes.stream().filter(n1 -> n1.getId().equals(n.getParentId())).findAny().orElse(null);
            return StringUtils.joinWith("/", storyMap.getName(), l1Node == null ? null : l1Node.getName(), n.getName());
        }));
    }

    public List<StoryMap> selectStoryMapByProjectId(Long projectId) {
        StoryMapExample example = new StoryMapExample();
        example.createCriteria().andProjectIdEqualTo(projectId);
        return storyMapMapper.selectByExample(example);
    }

    public List<StoryMapNode> selectNodeByStoryMapId(Long storyMapId) {
        StoryMapNodeExample example = new StoryMapNodeExample();
        example.createCriteria().andStoryMapIdEqualTo(storyMapId);
        return storyMapNodeMapper.selectByExample(example);
    }

    public List<StoryMapNode> selectL2NodeByStoryMapId(Long storyMapId) {
        StoryMapNodeExample example = new StoryMapNodeExample();
        example.createCriteria().andStoryMapIdEqualTo(storyMapId).andParentIdGreaterThan(0L);
        return storyMapNodeMapper.selectByExample(example);
    }

    public List<StoryMapNode> selectNodeByParent(Long parentStoryMapNodeId) {
        StoryMapNodeExample example = new StoryMapNodeExample();
        example.createCriteria().andParentIdEqualTo(parentStoryMapNodeId);
        return storyMapNodeMapper.selectByExample(example);
    }
}
