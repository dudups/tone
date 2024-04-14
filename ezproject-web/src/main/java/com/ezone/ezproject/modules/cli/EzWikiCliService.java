package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.modules.cli.bean.WikiSpace;
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

@Service
@Slf4j
public class EzWikiCliService {

    @Value("${internal.ezwiki.endpoint}")
    private String endpoint;

    @Value("${internal.ezwiki.token}")
    private String token;

    public WikiSpace checkedSpace(Long companyId, String user, Long spaceId) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path(String.format("/internal/api/space/checkedSpace/%d", spaceId))
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("companyId", String.valueOf(companyId))
                .param("user", user)
                .get(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<WikiSpace>>() {});
    }

    public List<WikiSpace> getSpaces(List<Long> spaceIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/api/space")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("ids", StringUtils.join(spaceIds, ","))
                .get(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<List<WikiSpace>>>() {});
    }

    public Object listWikiPageAndSpaces(List<Long> pageIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/api/page")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .jsonBody(pageIds)
                .post(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public List<Long> checkAndFilterWikiPage(String user, Long spaceId, List<Long> pageIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/api/page/checkAndFilter")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("user", user)
                .param("spaceId", String.valueOf(spaceId))
                .jsonBody(pageIds)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<List<Long>>>() {});
    }

    public Map<Long,List<Long>> checkAndFilterWikiPage(String user, Map<Long,List<Long>> spacePageIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/api/page/checkAndFilterOfMultiSpace")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("user", user)
                .jsonBody(spacePageIds)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<Map<Long,List<Long>>>>() {});
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求wiki服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求wiki返回失败：%s", response.getMessage()));
        }
    }

    private String md5(Long timestamp) {
        return DigestUtils.md5Hex(this.token + timestamp);
    }

    /**
     * 仅适用于，基础类型和List、Map，不适用于自定义Bean
     * @param responseStr
     * @param <T>
     * @return
     */
    private <T> T parseResponseData(String responseStr) {
        BaseResponse<T> response = JsonUtils.toObject(responseStr, new TypeReference<BaseResponse<T>>() {}, true);
        checkResponse(response);
        return response.getData();
    }

    private <T> T parseResponseData(String responseStr, TypeReference<BaseResponse<T>> typeReference) {
        BaseResponse<T> response = JsonUtils.toObject(responseStr, typeReference, true);
        checkResponse(response);
        return response.getData();
    }
}
