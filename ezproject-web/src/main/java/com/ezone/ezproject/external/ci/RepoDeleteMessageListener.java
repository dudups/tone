package com.ezone.ezproject.external.ci;

import com.ezone.ezproject.external.ci.bean.RepoEventMessage;
import com.ezone.ezproject.modules.project.service.ProjectRepoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@RocketMQMessageListener(
        topic = "${rocketmq.consumer.repoEvent.topic}",
        selectorExpression = "DELETE",
        consumerGroup = "${rocketmq.consumer.repoEvent.group}",
        consumeThreadMax = 10)
@Service
@AllArgsConstructor
@Slf4j
public class RepoDeleteMessageListener implements RocketMQListener<RepoEventMessage> {
    private ProjectRepoService projectRepoService;

    @Override
    public void onMessage(RepoEventMessage message) {
        log.info("received repo event message: {}", message);
        if (null == message) {
            return;
        }
        if (RepoEventMessage.OperationType.REPO_DELETE.equals(message.getOperationType())) {
            projectRepoService.deleteByRepoId(message.getRepoId());
        }
    }
}
