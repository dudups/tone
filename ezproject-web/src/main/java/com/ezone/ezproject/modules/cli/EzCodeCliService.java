package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.util.HttpClient;
import com.ezone.galaxy.framework.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@Deprecated
public class EzCodeCliService {

    @Value("${internal.ezcode.endpoint}")
    private String endpoint;

    @Value("${internal.ezcode.token}")
    private String token;

    public Object relateRepoMsg(Long companyId, String user, String projectKey, Long cardSeqNum, String msgType) {
        BaseResponse response = new HttpClient(endpoint)
                .path(String.format("/internal/card_relations/%d/%s-%d", companyId, projectKey, cardSeqNum))
                .param("type", msgType)
                .param("username", user)
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public void checkRepoOrDirPermission(Long companyId, String user, String path) {
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/project/users/permission")
                .param("companyId", String.valueOf(companyId))
                .param("username", user)
                .param("dirOrRepoName", path)
                .get(BaseResponse.class);
        checkResponse(response);
    }

    public List<String> getCheckedReposByRepoOrDir(Long companyId, String user, String path) {
        String response = new HttpClient(endpoint)
                .path("/internal/project/repos/filter")
                .param("companyId", String.valueOf(companyId))
                .param("username", user)
                .param("dirOrRepoName", path)
                .get(String.class);
        return parseResponseData(response);
    }

    public Object relateRepoOverview(Long companyId, String companyName, String user, List<String> repos, Long year, String dimension, Long value) {
        BaseResponse response = new HttpClient(endpoint)
                .path(String.format("/internal/stats/%d/overview", companyId))
                .param("companyName", companyName)
                .param("username", user)
                .param("repoNames", StringUtils.join(repos, ","))
                .param("year", String.valueOf(year))
                .param("dimension", dimension)
                .param("value", String.valueOf(value))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求code服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求code返回失败：%s", response.getMessage()));
        }
    }

    private <T> T parseResponseData(String responseStr) {
        BaseResponse<T> response = JsonUtils.toObject(responseStr, new TypeReference<BaseResponse<T>>() {}, true);
        checkResponse(response);
        return response.getData();
    }
}
