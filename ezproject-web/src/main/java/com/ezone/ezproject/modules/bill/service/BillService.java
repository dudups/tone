package com.ezone.ezproject.modules.bill.service;

import com.ezone.ezbase.iam.bean.mq.CompanyConsumptionMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class BillService {
    @Value("${rocketmq.producer.billTopic}")
    private String billTopic;

    private final RocketMQTemplate rocketMQTemplate;

    public void bill(CompanyConsumptionMsg msg) {
        rocketMQTemplate.asyncSend(billTopic, msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) { }

            @Override
            public void onException(Throwable e) {
                log.error(String.format("Send bill:[%s] exception!", msg), e);
            }
        });
    }
}
