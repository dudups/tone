package com.ezone.ezproject.modules.log.service;

import com.ezone.ezbase.iam.bean.enums.ResourceType;
import com.ezone.ezbase.iam.bean.enums.SystemType;
import com.ezone.ezbase.iam.bean.mq.CompanyAuditMessage;
import com.ezone.ezproject.dal.entity.Project;
import com.ezone.ezproject.modules.common.OperationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.ezone.ezbase.iam.bean.mq.CompanyAuditMessage.AuditType.RESOURCE_ADD;

@Service
@Slf4j
@RequiredArgsConstructor
@Async
public class CompanyAuditService {
    @Value("${rocketmq.producer.companyAuditTopic}")
    private String companyAuditTopic;

    private final RocketMQTemplate rocketMQTemplate;

    public void sendProjectCompanyAuditMessage(OperationContext context, Long userId, Project project, CompanyAuditMessage.AuditType auditType) {
        CompanyAuditMessage companyAuditMessage = new CompanyAuditMessage();
        companyAuditMessage.setCompanyId(project.getCompanyId());
        companyAuditMessage.setAccessIp(context.getIp());
        String opName = auditType.equals(RESOURCE_ADD) ? "创建" : "删除";
        companyAuditMessage.setDetail(String.format("%s%s项目", opName, project.getName()));
        companyAuditMessage.setResourceId(project.getId());
        companyAuditMessage.setResourceType(ResourceType.SCAN_PROJECT);
        companyAuditMessage.setSystemType(SystemType.EZPROJECT);
        companyAuditMessage.setType(auditType);
        companyAuditMessage.setUserId(userId);
        sendCompanyAuditMessage(companyAuditMessage);
    }

    public void sendCompanyAuditMessage(CompanyAuditMessage companyMessage) {
        rocketMQTemplate.asyncSend(companyAuditTopic, companyMessage, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
            }

            @Override
            public void onException(Throwable e) {
                log.error(String.format("send CompanyAuditMessage error!, messageDetail=%s", companyMessage.toString()), e);
            }
        });
    }

}
