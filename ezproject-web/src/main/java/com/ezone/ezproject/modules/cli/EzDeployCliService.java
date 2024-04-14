package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.modules.cli.bean.ResourceHostGroup;
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

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EzDeployCliService {

    @Value("${internal.ezdeploy.endpoint}")
    private String endpoint;

    @Value("${internal.ezdeploy.token}")
    private String token;

    public ResourceHostGroup checkedHostGroup(Long companyId, String user, Long groupId) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path(String.format("/internal/groups/%d", groupId))
                // todo 以下是ezdeploy暂不支持的参数
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("companyId", String.valueOf(companyId))
                .param("user", user)
                .get(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<ResourceHostGroup>>() {});
    }

    public Map<Long, ResourceHostGroup> getHostGroups(Long companyId, List<Long> groupIds) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/groups/get_by_ids")
                // todo 以下是ezdeploy暂不支持的参数
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("companyId", String.valueOf(companyId))
                .param("groupIds", StringUtils.join(groupIds, ","))
                .get(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<Map<Long, ResourceHostGroup>>>() {});
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求deploy资源服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求deploy返回失败：%s", response.getMessage()));
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
