package com.ezone.ezproject.modules.storymap.controller;


import com.ezone.ezbase.iam.bean.enums.TokenAuthType;
import com.ezone.ezproject.common.auth.CheckAuthType;
import com.ezone.ezproject.common.controller.AbstractController;
import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.dal.entity.StoryMap;
import com.ezone.ezproject.dal.entity.StoryMapNode;
import com.ezone.ezproject.es.entity.enums.OperationType;
import com.ezone.ezproject.modules.card.bean.SearchEsRequest;
import com.ezone.ezproject.modules.storymap.service.StoryMapCmdService;
import com.ezone.ezproject.modules.storymap.service.StoryMapQueryService;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.List;

@ApiOperation("故事地图")
@RestController
@RequestMapping("/project/story-map")
@Slf4j
@AllArgsConstructor
public class StoryMapController extends AbstractController {
    private StoryMapQueryService storyMapQueryService;
    private StoryMapCmdService storyMapCmdService;

    @ApiOperation("新建故事地图")
    @PostMapping
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<StoryMap> createStoryMap(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId,
                                                 @ApiParam(value = "故事地图名称") @Validated @Size(min = 1, max = 32, message = "名称长度必须在1和32之间") @RequestParam String name) {
        checkPermission(projectId, OperationType.STORY_MAP_CREATE);
        return success(storyMapCmdService.createStoryMap(projectId, name));
    }

    @ApiOperation("新建故事地图分类节点")
    @PostMapping("{storyMapId:[0-9]+}/node")
    @CheckAuthType(TokenAuthType.WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<StoryMapNode> createStoryMapNode(@ApiParam(value = "故事地图ID", example = "1") @PathVariable Long storyMapId,
                                                         @ApiParam(value = "新建分类名称") @Validated @Size(min = 1, max = 32, message = "名称长度必须在1和32之间") @RequestParam String name,
                                                         @ApiParam(value = "父分类ID，L1级别父为0", defaultValue = "0") @RequestParam Long parentId,
                                                         @ApiParam(value = "位于哪个兄弟分类ID之后，同级尚无分类则为0", defaultValue = "0") @RequestParam Long afterId) {
        StoryMap storyMap = storyMapQueryService.selectStoryMapById(storyMapId);
        if (null == storyMap) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMap.getProjectId(), OperationType.STORY_MAP_CREATE);
        return success(storyMapCmdService.createStoryMapNode(storyMap, name, parentId, afterId));
    }

    @ApiOperation("更新故事地图")
    @PutMapping("{storyMapId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<StoryMap> updateStoryMap(@ApiParam(value = "故事地图ID", example = "1") @PathVariable Long storyMapId,
                                                 @ApiParam(value = "故事地图名称") @Validated @Size(min = 1, max = 32, message = "名称长度必须在1和32之间") @RequestParam String name) {
        StoryMap storyMap = storyMapQueryService.selectStoryMapById(storyMapId);
        if (null == storyMap) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMap.getProjectId(), OperationType.STORY_MAP_CREATE);
        return success(storyMapCmdService.updateStoryMap(storyMapId, name));
    }

    @ApiOperation("更新故事地图未规划卡片池的查询条件")
    @PutMapping("{storyMapId:[0-9]+}/query")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse updateStoryMapQuery(
            @ApiParam(value = "故事地图ID", example = "1") @PathVariable Long storyMapId,
            @ApiParam(value = "故事地图名称") @RequestBody SearchEsRequest query) throws Exception {
        StoryMap storyMap = storyMapQueryService.selectStoryMapById(storyMapId);
        if (null == storyMap) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMap.getProjectId(), OperationType.STORY_MAP_CREATE);
        storyMapCmdService.saveStoryMapQuery(storyMapId, query);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("查询故事地图未规划卡片池的查询条件")
    @GetMapping("{storyMapId:[0-9]+}/query")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<SearchEsRequest> selectStoryMapQuery(
            @ApiParam(value = "故事地图ID", example = "1") @PathVariable Long storyMapId) throws Exception {
        StoryMap storyMap = storyMapQueryService.selectStoryMapById(storyMapId);
        if (null == storyMap) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(storyMap.getProjectId());
        return success(storyMapQueryService.selectStoryMapQuery(storyMapId));
    }

    @ApiOperation("更新故事地图分类")
    @PutMapping("node/{storyMapNodeId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<StoryMapNode> updateStoryMapNode(@ApiParam(value = "故事地图分类ID", example = "1") @PathVariable Long storyMapNodeId,
                                                         @ApiParam(value = "故事地图名称") @Validated @Size(min = 1, max = 32, message = "名称长度必须在1和32之间") @RequestParam String name) {
        StoryMapNode storyMapNode = storyMapQueryService.selectStoryMapNodeById(storyMapNodeId);
        if (null == storyMapNode) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMapNode.getProjectId(), OperationType.STORY_MAP_CREATE);
        return success(storyMapCmdService.updateStoryMapNode(storyMapNodeId, name));
    }

    @ApiOperation("移动故事地图分类")
    @PutMapping("node/{storyMapNodeId:[0-9]+}/move")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse<StoryMapNode> moveStoryMapNode(@ApiParam(value = "故事地图分类ID", example = "1") @PathVariable Long storyMapNodeId,
                                                       @ApiParam(value = "父L1分类ID，afterId为0时用于为L2指定父L1", defaultValue = "0") @RequestParam Long parentL1Id,
                                                       @ApiParam(value = "位于哪个兄弟分类ID之后，开头则为0", defaultValue = "0") @RequestParam Long afterId) {
        StoryMapNode storyMapNode = storyMapQueryService.selectStoryMapNodeById(storyMapNodeId);
        if (null == storyMapNode) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMapNode.getProjectId(), OperationType.STORY_MAP_CREATE);
        return success(storyMapCmdService.moveStoryMapNode(storyMapNode, parentL1Id, afterId));
    }

    @ApiOperation("删除故事地图")
    @DeleteMapping("{storyMapId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteStoryMap(@ApiParam(value = "故事地图ID", example = "1") @PathVariable Long storyMapId)
            throws IOException {
        StoryMap storyMap = storyMapQueryService.selectStoryMapById(storyMapId);
        if (null == storyMap) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMap.getProjectId(), OperationType.STORY_MAP_CREATE);
        storyMapCmdService.deleteStoryMap(storyMapId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("删除故事地图分类")
    @DeleteMapping("node/{storyMapNodeId:[0-9]+}")
    @CheckAuthType(TokenAuthType.WRITE)
    public BaseResponse deleteStoryMapNode(@ApiParam(value = "故事地图分类ID", example = "1") @PathVariable Long storyMapNodeId)
            throws IOException {
        StoryMapNode storyMapNode = storyMapQueryService.selectStoryMapNodeById(storyMapNodeId);
        if (null == storyMapNode) {
            throw CodedException.NOT_FOUND;
        }
        checkPermission(storyMapNode.getProjectId(), OperationType.STORY_MAP_CREATE);
        storyMapCmdService.deleteStoryMapNode(storyMapNodeId);
        return SUCCESS_RESPONSE;
    }

    @ApiOperation("故事地图")
    @GetMapping
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<StoryMap>> selectStoryMapByProjectId(@ApiParam(value = "项目ID", example = "1") @RequestParam Long projectId) {
        checkHasProjectRead(projectId);
        return success(storyMapQueryService.selectStoryMapByProjectId(projectId));
    }

    @ApiOperation("故事地图分类")
    @GetMapping("{storyMapId:[0-9]+}/node")
    @CheckAuthType(TokenAuthType.READ)
    public BaseResponse<List<StoryMapNode>> selectNodeByStoryMapId(@ApiParam(value = "故事地图分类ID", example = "1") @PathVariable Long storyMapId) {
        StoryMap storyMap = storyMapQueryService.selectStoryMapById(storyMapId);
        if (null == storyMap) {
            throw CodedException.NOT_FOUND;
        }
        checkHasProjectRead(storyMap.getProjectId());
        return success(storyMapQueryService.selectNodeByStoryMapId(storyMapId));
    }
}
