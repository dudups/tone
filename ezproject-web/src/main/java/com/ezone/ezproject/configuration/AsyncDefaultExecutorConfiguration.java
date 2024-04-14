package com.ezone.ezproject.configuration;

import com.ezone.galaxy.framework.common.concurrent.CommonExecutor;
import lombok.AllArgsConstructor;
import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@AllArgsConstructor
@Configuration
public class AsyncDefaultExecutorConfiguration {
    private CommonExecutor commonExecutor;

    @Bean(AsyncExecutionAspectSupport.DEFAULT_TASK_EXECUTOR_BEAN_NAME)
    public TaskExecutor defaultTaskExecutor() {
        return task -> commonExecutor.execute(task);
    }
}
