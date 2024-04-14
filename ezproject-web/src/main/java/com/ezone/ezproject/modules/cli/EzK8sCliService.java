package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.ez.context.CompanyService;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.util.HttpClient;
import com.ezone.galaxy.framework.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class EzK8sCliService {

    @Value("${internal.ezk8s.endpoint}")
    private String endpoint;

    @Value("${internal.ezk8s.token}")
    private String token;

    @Autowired
    private CompanyService companyService;

    public Object getSpaces(List<Long> k8sGroupIds) {
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/manager/clusters")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("clusterIds", StringUtils.join(k8sGroupIds, ","))
                .param("companyId", companyService.currentCompany().toString())
                .get(BaseResponse.class);
        checkResponse(response);
        return response.getData();
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求k8s管理服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), String.format("请求k8s管理服务返回失败：%s", response.getMessage()));
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
