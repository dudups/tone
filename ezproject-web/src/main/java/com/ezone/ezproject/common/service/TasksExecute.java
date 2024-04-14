package com.ezone.ezproject.common.service;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 该类设计用于处理io密集型操作，如http请求，复杂查询。
 * 使用示例
 *    List<TasksExecute.Result> results = TasksExecute.builder()
 *    .executorService(ThreadPool.ioExecutorService) // 设置执行任务的线程池
 *   .errorPolicy(TaskExecuteErrorPolicy.CONTINUE_ON_ERROR) // 设置错误处理策略
 *   .tasks(suppliers) //设置待执行的任务
 *   .timeoutSeconds(5) //设置超时时间，不设置默认一直等待
 *   .timeOutPolicy(TaskExecuteTimeOutPolicy.THROW_TIMEOUT_ERROR) //设置当任务执行超时时的处理策略，默认为NORMAL。即不抛异常，并获取已经执行完任务的结果。
 *   .build()
 *   .submit();  //开始执行，并等待返回
 */
@Slf4j
@RequiredArgsConstructor
public class TasksExecute {

    private final ExecutorService executorService;

    /**
     *  待执行的任务（函数）
     */
    private final List<Supplier<?>> suppliers;

    /**
     * 所有任务结束后的回调函数
     */
    private final Consumer<List<Result>> finishConsumer;

    /**
     * 异常策略
     */
    private final TaskExecuteErrorPolicy errorPolicy;

    /**
     * 超时策略，默认为NORMAL。即不抛异常，并获取已经执行完任务的结果。
     */
    private final TaskExecuteTimeOutPolicy timeOutPolicy;

    /**
     * 超时时间设置
     */
    private final int timeoutSeconds;


    private CountDownLatch countDownLatch = null;
    private List<Result> results = null;
    private List<Future<?>> futures = null;
    private AtomicInteger success = new AtomicInteger(0);

    public static TasksExecuteBuilder builder() {
        return new TasksExecuteBuilder();
    }


    private void init() {
        int size = suppliers.size();
        countDownLatch = new CountDownLatch(size);
        futures = new ArrayList<>(size);
        results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            results.add(Result.builder().build());
        }
    }

    public List<Result> submit() {
        init();
        debugThreadPoolInfo();
        List<Result> results;
        switch (errorPolicy) {
            case RETURN_ON_TASK_ERROR:
                results = execOnReturnOnTaskErrorModel();
                break;
            case CONTINUE_ON_ERROR:
                results = execOnContinueOnErrorModel();
                break;
            case RETURN_ON_SUBMIT_ERROR:
                results =  execOnReturnOnSubmitErrorModel();
                break;
            default:
                results = Collections.emptyList();
        }
        debugThreadPoolInfo();
        return results;
    }

    private void debugThreadPoolInfo() {
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            log.debug(String.format("队列线程数：%d,活动线程数%d，完成任务数%d", tpe.getQueue().size(),tpe.getActiveCount(),tpe.getCompletedTaskCount()));
        }
    }

    private List<Result> execOnReturnOnTaskErrorModel() {
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier<?> supplier = suppliers.get(i);
            final int currentIndex = i;
            try {
                Future<?> taskFuture = executorService.submit(() -> {
                    try {
                        results.get(currentIndex).setContent(supplier.get());
                        success.addAndGet(1);
                    } catch (Exception e) {
                        log.error("[execOnContinueOnErrorModel][error][" + e.getMessage() + "]", e);
                        if (results.get(currentIndex).getException() == null) {
                            results.get(currentIndex).setException(e);
                        }
                        //任务执行失败就取消所有任务。
                        cancelAllTask();
                    } finally {
                        countDownLatch.countDown();
                    }
                });
                futures.add(taskFuture);
            } catch (Exception e) {
                //提交失败时，取消业务相关所有提交
                log.error("[execOnReturnOnTaskErrorModel][" + " timeoutSeconds :" + timeoutSeconds + "][error][" + e.getMessage() + "]", e);
                log.info("任务提交失败，取消全部任务！");
                cancelAllTask();
                results.get(currentIndex).setException(e);
                return results;
            }
        }
        waitAllCompletion(finishConsumer);
        return results;
    }

    public List<Result> execOnContinueOnErrorModel() {
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier<?> supplier = suppliers.get(i);
            Future<?> taskFuture;
            final int currentIndex = i;
            try {
                taskFuture = executorService.submit(() -> {
                    try {
                        Object result = supplier.get();
                        results.get(currentIndex).setContent(result);
                        success.addAndGet(1);
                    } catch (Exception e) {
                        log.error("[execOnContinueOnErrorModel][error][" + e.getMessage() + "]", e);
                        if (results.get(currentIndex).getException() == null) {
                            results.get(currentIndex).setException(e);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
                futures.add(taskFuture);
            } catch (Exception e) {
                log.error("[execOnContinueOnErrorModel][" + " timeoutSeconds :" + timeoutSeconds + "][error][" + e.getMessage() + "]", e);
                //提交失败时，取消业务相关所有提交
                log.info("任务提交失败，其他任务继续！");
                countDownLatch.countDown();
                results.get(currentIndex).setException(e);
            }
        }
        waitAllCompletion(finishConsumer);
        return results;
    }

    private List<Result> execOnReturnOnSubmitErrorModel() {
        for (int i = 0; i < suppliers.size(); i++) {
            Supplier<?> supplier = suppliers.get(i);
            final int currentIndex = i;
            try {
                Future<?> taskFuture = executorService.submit(() -> {
                    try {
                        Object result = supplier.get();
                        results.get(currentIndex).setContent(result);
                        success.addAndGet(1);
                    } catch (Exception e) {
                        log.error("[execOnReturnOnSubmitErrorModel][error][" + e.getMessage() + "]", e);
                        if (results.get(currentIndex).getException() == null) {
                            results.get(currentIndex).setException(e);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
                futures.add(taskFuture);
            } catch (Exception e) {
                //提交失败时，取消业务相关所有提交
                log.error("[execOnReturnOnSubmitErrorModel][error][" + e.getMessage() + "]", e);
                log.info("任务提交失败，取消全部任务！");
                cancelAllTask();
                log.info("取消完成！添加异常");
                results.get(currentIndex).setException(e);
                return results;
            }
        }
        waitAllCompletion(finishConsumer);
        return results;
    }

    public void waitAllCompletion(Consumer<List<Result>> consumer) {
        try {
            boolean completed;
            if (timeoutSeconds <= 0) {
                countDownLatch.await();
                completed = true;
            } else {
                completed = countDownLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            }

            if (completed) {
                if (consumer != null) {
                    consumer.accept(results);
                }
            } else {
                cancelAllTask();
                if (timeOutPolicy != null && TaskExecuteTimeOutPolicy.THROW_TIMEOUT_ERROR.equals(timeOutPolicy)) {
                    throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, String.format("超时！%s秒未处理完成！", timeoutSeconds));
                }
            }
        } catch (InterruptedException e) {
            log.error("[waitAllCompletion][" + " timeoutSeconds :" + timeoutSeconds + "; consumer :" + consumer + "][error][" + e.getMessage() + "]", e);
            cancelAllTask();
            Thread.currentThread().interrupt();
            throw new CodedException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void cancelAllTask() {
        int cancelCount = 0;
        for (Future<?> future : futures) {
            if (!future.isDone()) {
                future.cancel(true);
                cancelCount++;
            }
        }
        log.info(String.format("成功了%s个任务，执行取消%s个任务！", success.get(), cancelCount));
        long count = countDownLatch.getCount();
        for (long i = 0; i < count; i++) {
            countDownLatch.countDown();
        }
    }

    @Data
    @Builder
    public static class Result {
        private Object content;
        private Throwable exception;

        Result(Object content, Throwable exception) {
            this.content = content;
            this.exception = exception;
        }
    }

    public static class TasksExecuteBuilder {
        private ExecutorService executorService;
        private List<Supplier<?>> suppliers;
        private Consumer<List<Result>> finishConsumer;
        private TaskExecuteErrorPolicy errorPolicy;
        private TaskExecuteTimeOutPolicy timeOutPolicy;
        private int timeoutSeconds;

        TasksExecuteBuilder() {
        }

        public TasksExecuteBuilder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public TasksExecuteBuilder tasks(List<Supplier<?>> suppliers) {
            this.suppliers = suppliers;
            return this;
        }

        public TasksExecuteBuilder errorPolicy(TaskExecuteErrorPolicy errorPolicy) {
            this.errorPolicy = errorPolicy;
            return this;
        }

        public TasksExecuteBuilder timeOutPolicy(TaskExecuteTimeOutPolicy timeOutPolicy) {
            this.timeOutPolicy = timeOutPolicy;
            return this;
        }

        public TasksExecuteBuilder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public TasksExecute build() {
            return new TasksExecute(executorService, suppliers, finishConsumer, errorPolicy, timeOutPolicy, timeoutSeconds);
        }

        @Override
        public String toString() {
            return "TasksExecute.TasksExecuteBuilder(executorService=" + this.executorService + ", suppliers=" + this.suppliers + ", finishConsumer=" + this.finishConsumer + ", errorPolicy=" + this.errorPolicy + ", timeOutPolicy=" + this.timeOutPolicy + ", timeoutSeconds=" + this.timeoutSeconds  + ")";
        }
    }
}

