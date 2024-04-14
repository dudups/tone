package com.ezone.ezproject.modules.cli;

import com.ezone.ezproject.common.exception.CodedException;
import com.ezone.ezproject.common.transactional.AfterCommit;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlow;
import com.ezone.ezproject.modules.cli.bean.CreateBpmFlowResult;
import com.ezone.ezproject.modules.cli.bean.CreateWorkloadBpmFlow;
import com.ezone.ezproject.modules.common.InternalApiAuthentication;
import com.ezone.galaxy.framework.common.bean.BaseResponse;
import com.ezone.galaxy.framework.common.util.HttpClient;
import com.ezone.galaxy.framework.common.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EzBpmCliService {

    @Value("${internal.ezbpm.endpoint}")
    private String endpoint;

    @Value("${internal.ezbpm.token}")
    private String token;

    public CreateBpmFlowResult createFlow(CreateBpmFlow flow) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/approval/project")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .jsonBody(flow)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<CreateBpmFlowResult>>() {});
    }

    public CreateBpmFlowResult createFlow(CreateWorkloadBpmFlow flow) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                // todo
                .path("/internal/approval/project")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .jsonBody(flow)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<CreateBpmFlowResult>>() {});
    }

    public Map<Long, CreateBpmFlowResult> batchCreateFlow(List<CreateBpmFlow> flows) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/approval/project/batch")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .jsonBody(flows)
                .post(String.class);
        return parseResponseData(responseStr, new TypeReference<BaseResponse<Map<Long,CreateBpmFlowResult>>>() {});
    }

    public Object getFlows(List<Long> flowIds, Long companyId, String user) {
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path("/internal/approval/detail")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("approvalIds", StringUtils.join(flowIds, ","))
                .param("companyId", String.valueOf(companyId))
                .param("username", user)
                .get(String.class);
        return parseResponseData(responseStr);
    }

    public Object getFlow(Long flowId) {
        if (flowId == null || flowId < 0) {
            return null;
        }
        Long timestamp = System.currentTimeMillis();
        String responseStr = new HttpClient(endpoint)
                .path(String.format("/internal/approval/%d", flowId))
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .get(String.class);
        return parseResponseData(responseStr);
    }

    @AfterCommit
    @Async
    public void asyncCancelFlow(Long flowId, String user) {
        cancelFlow(flowId, user);
    }

    public void cancelFlow(Long flowId, String user) {
        if (flowId == null || flowId <= 0) {
            return;
        }
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/approval/revoke")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("approvalId", String.valueOf(flowId))
                .param("username", user)
                .put(BaseResponse.class);
        checkResponse(response);
    }

    public void cancelFlows(List<Long> flowIds, String user) {
        if (CollectionUtils.isEmpty(flowIds)) {
            return;
        }
        Long timestamp = System.currentTimeMillis();
        BaseResponse response = new HttpClient(endpoint)
                .path("/internal/approval/batchRevoke")
                .header(InternalApiAuthentication.HEADER_TIMESTAMP, String.valueOf(timestamp))
                .header(InternalApiAuthentication.HEADER_MD5, md5(timestamp))
                .param("approvalIds", StringUtils.join(flowIds, ","))
                .param("username", user)
                .put(BaseResponse.class);
        checkResponse(response);
    }

    private void checkResponse(BaseResponse response) {
        if (null == response) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "请求bpm服务异常！");
        }
        if (response.isError()) {
            throw new CodedException(response.getCode(), response.getMessage());
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
