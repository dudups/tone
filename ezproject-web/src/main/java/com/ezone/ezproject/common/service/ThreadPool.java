package com.ezone.ezproject.common.service;

import com.ezone.ezproject.common.exception.CodedException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 */
public class ThreadPool {
    protected static final int POOL_SIZE = 2000;

    protected static final int MAX_POOL_SIZE = 20000;
    /***
     * 用于处理IO任务的线程池
     */
    public static ExecutorService ioExecutorService = new ThreadPoolExecutor(POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1), r -> {
        Thread thread = new Thread(r);
        thread.setName("ioExecutorService");
        return thread;
    }, new MyAbortPolicy());

    /***
     * 单线程线程池，
     */
    public static ExecutorService singleThreadExecutorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1), r -> {
        Thread thread = new Thread(r);
        thread.setName("singleThreadExecutorService");
        return thread;
    }, new MyAbortPolicy());

    private static class MyAbortPolicy implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, "Task " + r.toString() +
                    " 由于没有线程资源进行处理，已经被拒绝！");
        }
    }
}
