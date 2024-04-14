package com.ezone.ezproject.modules.storymap.service;

import com.ezone.ezproject.common.IdUtil;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.Card;
import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapExample;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.dal.entity.StoryMapNodeExample;
import com.ezone.ezproject.dal.mapper.StoryMapMapper;
import com.ezone.ezproject.dal.mapper.StoryMapNodeMapper;
import com.ezone.ezproject.es.dao.StoryMapQueryDao;
import com.ezone.ezproject.ez.context.UserService;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.card.service.CardCmdService;
import com.ezone.ezproject.modules.card.service.CardQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Transactional(rollbackFor = Exception.class)
@Service
@Slf4j
@AllArgsConstructor
public class StoryMapCmdService {
    private StoryMapMapper storyMapMapper;
    private StoryMapNodeMapper storyMapNodeMapper;
    private StoryMapQueryDao storyMapQueryDao;

    private StoryMapQueryService storyMapQueryService;

    private UserService userService;
    private CardQueryService cardQueryService;
    private CardCmdService cardCmdService;

    private static final Map<Long, String> INIT_L1_NODES = new HashMap<Long, String>() {{
        put(0L, "一级分类");
        put(1L, "一级分类2");
    }};

    private static final Map<Long, String> INIT_L2_NODES = new HashMap<Long, String>() {{
        put(0L, "二级分类");
        put(1L, "二级分类2");
    }};

    public StoryMap createStoryMap(Long projectId, String name) {
        String user = userService.currentUserName();
        StoryMap storyMap = StoryMap.builder()
                .id(IdUtil.generateId())
                .projectId(projectId)
                .name(name)
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
        storyMapMapper.insert(storyMap);
        initL1Node(user, storyMap).forEach(n -> initL2Node(user, n));
        return storyMap;
    }

    public void saveStoryMapQuery(Long storyMapId, SearchEsRequest request) throws IOException {
        storyMapQueryDao.saveOrUpdate(storyMapId, request);
    }

    public StoryMapNode createStoryMapNode(StoryMap storyMap, String name, Long parentId, Long afterId) {
        String user = userService.currentUserName();
        List<StoryMapNode> nodes = storyMapQueryService.selectNodeByStoryMapId(storyMap.getId());
        if (null != parentId && parentId > 0) {
            StoryMapNode parentNode = nodes.stream().filter(n -> n.getId().equals(parentId)).findAny().orElse(null);
            if (null == parentNode) {
                throw new CodedException(HttpStatus.NOT_FOUND, "父分类节点不存在!");
            }
        }
        Long seqIndex;
        if (null != afterId && afterId > 0) {
            StoryMapNode afterNode = nodes.stream().filter(n -> n.getId().equals(afterId)).findAny().orElse(null);
            if (null == afterNode) {
                throw new CodedException(HttpStatus.NOT_FOUND, "同级定位节点不存在!");
            } else {
                seqIndex = afterNode.getSeqIndex() + 1;
            }
        } else {
            seqIndex = 0L;
        }
        StoryMapNode node = StoryMapNode.builder()
                .id(IdUtil.generateId())
                .projectId(storyMap.getProjectId())
                .storyMapId(storyMap.getId())
                .name(name)
                .seqIndex(seqIndex)
                .parentId(parentId)
                .createUser(user)
                .createTime(new Date())
                .lastModifyUser(user)
                .lastModifyTime(new Date())
                .build();
        storyMapNodeMapper.insert(node);
        nodes.stream()
                .filter(n -> n.getParentId().equals(null == parentId ? 0L : parentId))
                .filter(n -> n.getSeqIndex() >= seqIndex)
                .forEach(n -> {
                    n.setSeqIndex(n.getSeqIndex() + 1);
                    storyMapNodeMapper.updateByPrimaryKey(n);
                });
        return node;
    }

    public StoryMapNode moveStoryMapNode(StoryMapNode moveNode, Long parentL1Id, Long afterId) {
        if (moveNode.getId().equals(afterId)) {
            return moveNode;
        }
        List<StoryMapNode> nodes = storyMapQueryService.selectNodeByStoryMapId(moveNode.getStoryMapId());
        Consumer<StoryMapNode> incrSeqIndex = n -> {
            n.setSeqIndex(n.getSeqIndex() + 1);
            storyMapNodeMapper.updateByPrimaryKey(n);
        };
        Consumer<StoryMapNode> reduceSeqIndex = n -> {
            n.setSeqIndex(n.getSeqIndex() - 1);
            storyMapNodeMapper.updateByPrimaryKey(n);
        };
        StoryMapNode node = moveNode;
        // ...node...
        if (afterId == null || afterId == 0) {
            if (parentL1Id == null || parentL1Id == 0 || parentL1Id.equals(node.getParentId()) || node.getParentId() == 0) {
                nodes.stream()
                        .filter(n -> n.getParentId().equals(node.getParentId()))
                        .filter(n -> n.getSeqIndex() < node.getSeqIndex())
                        .forEach(incrSeqIndex);
                node.setSeqIndex(0L);
                storyMapNodeMapper.updateByPrimaryKey(node);
            } else {
                StoryMapNode toParent = nodes.stream().filter(n -> n.getId().equals(parentL1Id)).findAny().orElse(null);
                if (toParent == null) {
                    throw new CodedException(HttpStatus.NOT_FOUND, "指定父L1分类未找到!");
                } else if (toParent.getParentId() > 0) {
                    throw new CodedException(HttpStatus.NOT_FOUND, "指定父分类不能是L2分类!");
                }
                // ...node...   ...
                nodes.stream()
                        .filter(n -> n.getParentId().equals(node.getParentId()))
                        .filter(n -> n.getSeqIndex() > node.getSeqIndex())
                        .forEach(reduceSeqIndex);
                nodes.stream()
                        .filter(n -> n.getParentId().equals(parentL1Id))
                        .forEach(incrSeqIndex);
                node.setSeqIndex(0L);
                node.setParentId(parentL1Id);
                storyMapNodeMapper.updateByPrimaryKey(node);
            }
            return node;
        }
        StoryMapNode afterNode = nodes.stream().filter(n -> n.getId().equals(afterId)).findAny().orElse(null);
        if (afterNode == null) {
            throw new CodedException(HttpStatus.NOT_FOUND, "指定的定位兄弟分类未找到!");
        }
        StoryMapNode after = afterNode;
        if (node.getParentId().equals(afterNode.getParentId())) {
            if (node.getSeqIndex() == afterNode.getSeqIndex() + 1) {
                return node;
            }
            Long fromSeqIndex = node.getSeqIndex();
            Long afterSeqIndex = afterNode.getSeqIndex();
            if (fromSeqIndex < afterSeqIndex) {
                // ...node...after...
                nodes.stream()
                        .filter(n -> n.getParentId().equals(node.getParentId()))
                        .filter(n -> n.getSeqIndex() > fromSeqIndex && n.getSeqIndex() <= afterSeqIndex)
                        .forEach(reduceSeqIndex);
                node.setSeqIndex(afterSeqIndex);
            } else {
                // ...after...node...
                nodes.stream()
                        .filter(n -> n.getParentId().equals(node.getParentId()))
                        .filter(n -> n.getSeqIndex() > afterSeqIndex && n.getSeqIndex() < fromSeqIndex)
                        .forEach(incrSeqIndex);
                node.setSeqIndex(afterSeqIndex + 1);
            }
            storyMapNodeMapper.updateByPrimaryKey(node);
        } else {
            // ...node...   ...after...
            Long fromSeqIndex = node.getSeqIndex();
            Long afterSeqIndex = afterNode.getSeqIndex();
            nodes.stream()
                    .filter(n -> n.getParentId().equals(node.getParentId()))
                    .filter(n -> n.getSeqIndex() > fromSeqIndex)
                    .forEach(reduceSeqIndex);
            nodes.stream()
                    .filter(n -> n.getParentId().equals(after.getParentId()))
                    .filter(n -> n.getSeqIndex() > afterSeqIndex)
                    .forEach(incrSeqIndex);
            node.setParentId(afterNode.getParentId());
            node.setSeqIndex(afterSeqIndex + 1);
            storyMapNodeMapper.updateByPrimaryKey(node);
        }
        return node;
    }

    public StoryMap updateStoryMap(Long storyMapId, String name) {
        StoryMap storyMap = storyMapMapper.selectByPrimaryKey(storyMapId);
        String user = userService.currentUserName();
        storyMap.setName(name);
        storyMap.setLastModifyUser(user);
        storyMap.setLastModifyTime(new Date());
        storyMapMapper.updateByPrimaryKey(storyMap);
        return storyMap;
    }

    public StoryMapNode updateStoryMapNode(Long storyMapNodeId, String name) {
        StoryMapNode storyMapNode = storyMapNodeMapper.selectByPrimaryKey(storyMapNodeId);
        String user = userService.currentUserName();
        storyMapNode.setName(name);
        storyMapNode.setLastModifyUser(user);
        storyMapNode.setLastModifyTime(new Date());
        storyMapNodeMapper.updateByPrimaryKey(storyMapNode);
        return storyMapNode;
    }

    public void deleteStoryMap(Long storyMapId) throws IOException {
        StoryMap storyMap = storyMapMapper.selectByPrimaryKey(storyMapId);
        if (null == storyMap) {
            return;
        }
        List<StoryMapNode> nodes = storyMapQueryService.selectNodeByStoryMapId(storyMapId);
        if (CollectionUtils.isNotEmpty(nodes)) {
            List<StoryMapNode> l2Nodes = nodes.stream().filter(n -> n.getParentId() > 0).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(l2Nodes)) {
                List<Card> cards = cardQueryService.selectByStoryMapNodeId(
                        l2Nodes.stream().map(n -> n.getId()).collect(Collectors.toList()));
                Map<Long, String> storyMapNodeInfo = l2Nodes.stream().collect(Collectors.toMap(n -> n.getId(), n -> {
                    StoryMapNode l1Node = nodes.stream().filter(n1 -> n1.getId().equals(n.getParentId())).findAny().orElse(null);
                    return StringUtils.joinWith("/", storyMap.getName(), l1Node == null ? null : l1Node.getName(), n.getName());
                }));
                cardCmdService.unBindStoryMap(cards, storyMapNodeInfo);
            }
        }
        StoryMapNodeExample example = new StoryMapNodeExample();
        example.createCriteria().andStoryMapIdEqualTo(storyMapId);
        storyMapNodeMapper.deleteByExample(example);
        storyMapMapper.deleteByPrimaryKey(storyMapId);
        storyMapQueryDao.delete(storyMapId);
    }

    public void deleteStoryMapNode(Long storyMapNodeId) throws IOException {
        StoryMapNode node = storyMapNodeMapper.selectByPrimaryKey(storyMapNodeId);
        if (node == null) {
            return;
        }
        if (node.getParentId().equals(0L)) {
            List<StoryMapNode> nodes = storyMapQueryService.selectNodeByParent(storyMapNodeId);
            if (CollectionUtils.isNotEmpty(nodes)) {
                throw new CodedException(HttpStatus.NOT_ACCEPTABLE, String.format("有%s个子分类!", nodes.size()));
            }
        } else {
            Map<Long, String> storyMapNodeInfo = new HashMap<>();
            StoryMap storyMap = storyMapMapper.selectByPrimaryKey(node.getStoryMapId());
            StoryMapNode l1 = storyMapNodeMapper.selectByPrimaryKey(node.getParentId());
            storyMapNodeInfo.put(node.getId(), StringUtils.joinWith("/", storyMap.getName(), l1.getName(), node.getName()));
            List<Card> cards = cardQueryService.selectByStoryMapNodeId(node.getId());
            cardCmdService.unBindStoryMap(cards, storyMapNodeInfo);
        }
        storyMapNodeMapper.deleteByPrimaryKey(storyMapNodeId);
    }

    public void deleteByProject(Long projectId) throws IOException {
        StoryMapNodeExample storyMapNodeExample = new StoryMapNodeExample();
        storyMapNodeExample.createCriteria().andProjectIdEqualTo(projectId);
        storyMapNodeMapper.deleteByExample(storyMapNodeExample);

        StoryMapExample storyMapExample = new StoryMapExample();
        storyMapExample.createCriteria().andProjectIdEqualTo(projectId);
        storyMapMapper.deleteByExample(storyMapExample);
    }

    private List<StoryMapNode> initL1Node(String user, StoryMap storyMap) {
        return INIT_L1_NODES.entrySet().stream().map(e -> {
            StoryMapNode node = StoryMapNode.builder()
                    .id(IdUtil.generateId())
                    .projectId(storyMap.getProjectId())
                    .storyMapId(storyMap.getId())
                    .name(e.getValue())
                    .seqIndex(e.getKey())
                    .parentId(0L)
                    .createUser(user)
                    .createTime(new Date())
                    .lastModifyUser(user)
                    .lastModifyTime(new Date())
                    .build();
            storyMapNodeMapper.insert(node);
            return node;
        }).collect(Collectors.toList());
    }

    private List<StoryMapNode> initL2Node(String user, StoryMapNode storyMapL1) {
        return INIT_L2_NODES.entrySet().stream().map(e -> {
            StoryMapNode node = StoryMapNode.builder()
                    .id(IdUtil.generateId())
                    .projectId(storyMapL1.getProjectId())
                    .storyMapId(storyMapL1.getStoryMapId())
                    .name(e.getValue())
                    .seqIndex(e.getKey())
                    .parentId(storyMapL1.getId())
                    .createUser(user)
                    .createTime(new Date())
                    .lastModifyUser(user)
                    .lastModifyTime(new Date())
                    .build();
            storyMapNodeMapper.insert(node);
            return node;
        }).collect(Collectors.toList());
    }
}
