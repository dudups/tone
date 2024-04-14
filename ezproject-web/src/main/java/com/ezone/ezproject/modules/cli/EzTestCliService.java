package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.modules.card.bean.BatchBindRequest;
import com.ezone.ezproject.modules.card.bean.BatchBindResponse;
import com.ezone.ezproject.modules.card.bean.query.BindType;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.util.HttpClient;
import com.ezone.galaxy.framework.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EzTestCliService {

    @Value("${internal.eztest.endpoint}")
    private String endpoint;

    @Value("${internal.eztest.token}")
    private String token;

    public Object listSpaces(Long projectId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/space/byProject")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("projectId", String.valueOf(projectId))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public Object listBugSpaces(Long projectId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/space/byBugProject")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("projectId", String.valueOf(projectId))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public Object listRequirementSpaces(Long projectId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/space/byRequirementProject")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("projectId", String.valueOf(projectId))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public List<Long> listBindCaseIds(Long cardId, Long spaceId) {
        Long timestamp = System.currentTimeMillis();
        String response = new HttpClient(endpoint)
                .path("/internal/api/case/bindCaseIds")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .param("spaceId", String.valueOf(spaceId))
                .get(String.class);
        return parseResponseData(response);
    }

    public Object listBindCases(Long cardId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/case/bindCase")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public void updateBindCases(Long cardId, Long spaceId, List<Long> caseIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/case/bindCase")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .param("spaceId", String.valueOf(spaceId))
                .param("caseIds", StringUtils.join(caseIds, ","))
                .put(BaseResponse.class);
        checkResponse(response);
    }

    public BatchBindResponse batchBindCases(Long cardId, BatchBindRequest request) {
        Long timestamp = System.currentTimeMillis();
        Map<Long, List<BatchBindRequest.RelateTarget>> spaceRefIdsMap = request.spaceRefIdsMap();
        StringBuilder errorMsg = new StringBuilder();
        Map<Long, List<Long>> spaceCaseIds = new HashMap<>();
        spaceRefIdsMap.forEach((spaceId, relateObjects) -> {
            spaceCaseIds.put(spaceId, relateObjects.stream().map(relate-> relate.getRelateTargetId()).collect(Collectors.toList()));
        });
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/case/bindCaseOfMultiSpace")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .jsonBody(spaceCaseIds)
                .put(BaseResponse.class);
        try {
            checkResponse(response);
        } catch (CodedException exception) {
            log.error("request /internal/api/case/bindCaseOfMultiSpace error!", exception);
            errorMsg.append(exception.getMessage());
        }
        return BatchBindResponse.builder().bindType(BindType.TEST_CASE).successAdd(listBindCases(cardId)).errorMsg(errorMsg.toString()).build();
    }

    public void unBindCases(Long cardId, List<Long> caseIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/case/unBindCase")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .param("caseIds", StringUtils.join(caseIds, ","))
                .delete(BaseResponse.class);
        checkResponse(response);
    }

    public Object listBugBindCaseRuns(Long bugId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/case/bindPlanCase")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("bugId", String.valueOf(bugId))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public Object listBindApis(Long cardId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/api/bindApi")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public void updateBindApis(Long cardId, Long spaceId, List<Long> apiIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/api/bindApi")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .param("spaceId", String.valueOf(spaceId))
                .param("apiIds", StringUtils.join(apiIds, ","))
                .put(BaseResponse.class);
        checkResponse(response);
    }

    public BatchBindResponse batchUpdateBindApis(Long cardId, BatchBindRequest request) {
        Long timestamp = System.currentTimeMillis();
        Map<Long, List<BatchBindRequest.RelateTarget>> spaceRefIdsMap = request.spaceRefIdsMap();
        Map<Long, List<Long>> spaceApiIds = new HashMap<>();
        spaceRefIdsMap.forEach((spaceId, relateObjects) -> {
            spaceApiIds.put(spaceId, relateObjects.stream().map(relate-> relate.getRelateTargetId()).collect(Collectors.toList()));
        });
        StringBuilder errorMsg = new StringBuilder();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/api/bindApiOfMultiSpace")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .jsonBody(spaceApiIds)
                .put(BaseResponse.class);
        try {
            checkResponse(response);
        } catch (CodedException exception) {
            errorMsg.append(exception.getMessage());
        }
        return BatchBindResponse.builder().bindType(BindType.TEST_API).successAdd(listBindApis(cardId)).errorMsg(errorMsg.toString()).build();
    }

    public void unBindApis(Long cardId, List<Long> apiIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/api/unBindApi")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("cardId", String.valueOf(cardId))
                .param("apiIds", StringUtils.join(apiIds, ","))
                .delete(BaseResponse.class);
        checkResponse(response);
    }

    public Object listPlanAndSpaces(List<Long> planIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/api/internal/plan")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("ids", StringUtils.join(planIds, ","))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public void checkPlanRead(String user, Long planId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path(String.format("/api/internal/plan/%s/checkRead", planId))
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("user", user)
                .get(BaseResponse.class);
        checkResponse(response);
    }

    public List<Long> checkAndFilterPlan(String user, Long spaceId, List<Long> planIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/api/internal/plan/checkAndFilter")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("user", user)
                .param("spaceId", String.valueOf(spaceId))
                .jsonBody(planIds)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<List<Long>>>() {
        });
    }

    public Map<Long, List<Long>> checkAndFilterPlan(String user, Map<Long, List<Long>> spacePlanIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/api/internal/plan/checkAndFilterMultiSpace")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("user", user)
                .jsonBody(spacePlanIds)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<Map<Long, List<Long>>>>() {
        });
    }

    private String md5(Long timestamp) {
        return DigestUtils.md5Hex(this.token + timestamp);
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求test服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求test返回失败：%s", response.getMessage()));
        }
    }

    /**
     * 仅适用于，基础类型和List、Map，不适用于自定义Bean
     *
     * @param responseStr
     * @return
     */
    private <T> T parseResponseData(String responseStr) {
        BaseResponse<T> response = JsonUtils.toObject(responseStr, new TypeReference<BaseResponse<T>>() {
        }, true);
        checkResponse(response);
        return response.getData();
    }

    private <T> T parseResponseData(String responseStr, TypeReference<BaseResponse<T>> typeReference) {
        BaseResponse<T> response = JsonUtils.toObject(responseStr, typeReference, true);
        checkResponse(response);
        return response.getData();
    }
}
