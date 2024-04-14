package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EzDocCliService {

    @Value("${internal.ezdoc.endpoint}")
    private String endpoint;

    @Value("${internal.ezdoc.token}")
    private String token;

    public void checkSpace(Long companyId, String user, Long spaceId) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/space/verify")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("companyId", String.valueOf(companyId))
                .param("spaceId", String.valueOf(spaceId))
                .param("username", user)
                .get(BaseResponse.class);
        checkResponse(response);
    }

    public Object getSpaces(Long companyId, List<Long> spaceIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/space/ids")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("companyId", String.valueOf(companyId))
                .param("spaceIds", StringUtils.join(spaceIds, ","))
                .get(String.class);
        return parseResponseData(responseStr);
    }

    public Object listDocAndSpaces(Map<Long, List<Long>> spaceDocIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/file/detail")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .jsonBody(spaceDocIds)
                .post(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    public Map<Long, List<Long>> checkAndFilterDoc(String user, Map<Long, List<Long>> spaceDocIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/file/filter")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("username", user)
                .jsonBody(spaceDocIds)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<Map<Long, List<Long>>>>() {
        });
    }

    public List<Long> checkAndFilterDoc(String user, Long spaceId, List<Long> docIds) {
        Map spaceDocIds = new HashMap<Long, List<Long>>();
        spaceDocIds.put(spaceId, docIds);
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/file/filter")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("username", user)
                .jsonBody(spaceDocIds)
                .post(String.class);
        Map<Long, List<Long>> longListMap = parseResponseData(responseStr, new TypeReference<BaseResponse<Map<Long, List<Long>>>>() {
        });
        if (longListMap != null && longListMap.containsKey(spaceId)) {
            return longListMap.get(spaceId);
        } else {
            return Collections.emptyList();
        }
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求doc服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求doc返回失败：%s", response.getMessage()));
        }
    }

    private String md5(Long timestamp) {
        return DigestUtils.md5Hex(this.token + timestamp);
    }

    /**
     * 仅适用于，基础类型和List、Map，不适用于自定义Bean
     *
     * @param responseStr
     * @param <T>
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
