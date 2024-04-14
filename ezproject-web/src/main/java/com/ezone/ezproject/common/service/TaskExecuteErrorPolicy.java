package com.ezone.ezproject.common.service;

public enum TaskExecuteErrorPolicy {
    /**
     * 遇到任意一个提交失败就立即返回。
     */
    RETURN_ON_SUBMIT_ERROR,

    /**
     * 当有任务执行异常时，立即返回，同时会尽量取消未完成的任务（要求任务能被取消）
     */
    RETURN_ON_TASK_ERROR,

    /**
     * 当有提交或由于异常而运行失败的任务时，继续等待他任务运行完成。
     */
    CONTINUE_ON_ERROR

}
