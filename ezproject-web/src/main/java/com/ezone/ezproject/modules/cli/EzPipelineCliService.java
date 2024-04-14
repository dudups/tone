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
public class EzPipelineCliService {

    @Value("${internal.ezpipeline.endpoint}")
    private String endpoint;

    @Value("${internal.ezpipeline.token}")
    private String token;

    public Object relateRepoBuildOverview(Long companyId, List<Long> repoIds, Long year, String dimension, Long value) {
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/measures/plugin_types")
                .param("companyId", String.valueOf(companyId))
                .param("repoIds", StringUtils.join(repoIds, ","))
                .param("year", String.valueOf(year))
                .param("dimension", dimension)
                .param("value", String.valueOf(value))
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求pipeline服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求pipeline返回失败：%s", response.getMessage()));
        }
    }

    private <T> T parseResponseData(String responseStr) {
        BaseResponse<T> response = JsonUtils.toObject(responseStr, new TypeReference<BaseResponse<T>>() {}, true);
        checkResponse(response);
        return response.getData();
    }
}
