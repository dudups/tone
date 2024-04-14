package com.ezone.ezproject.common.service;

public enum TaskExecuteTimeOutPolicy {

    /**
     * 超时时抛出异常
     */
    THROW_TIMEOUT_ERROR,

    /**
     * 超时时，获取已经执行完成的结果。
     */
    NORMAL

}
