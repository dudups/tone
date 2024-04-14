package com.ezone.ezproject.common.service;

import com.ezone.ezproject.common.exception.CodedException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class TasksExecuteTest {
    @Test
    void testSpeedTime() {
        long start = System.currentTimeMillis();
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> {
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "first supplier";
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "second supplier";
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(3 * 1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "third supplier";
        });
        List<TasksExecute.Result> results = TasksExecute.builder()
                .executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_SUBMIT_ERROR)
                .timeoutSeconds(4)
                .tasks(suppliers).build()
                .submit();
        TasksExecute.Result result = results.get(0);
        assertEquals("first supplier", result.getContent());
        TasksExecute.Result result2 = results.get(1);
        assertEquals("second supplier", result2.getContent());
        TasksExecute.Result result3 = results.get(2);
        assertEquals("third supplier", result3.getContent());
        assertTrue(System.currentTimeMillis() - start < 4000);

    }

    @Test
    void testTaskException() {
        String errMessage = "这里有运行时异常！！";
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> "first supplier");
        suppliers.add(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException();
            }
            return "second supplier";
        });
        suppliers.add(() -> "third supplier");
        suppliers.add(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException(errMessage);
            }
            throw new RuntimeException(errMessage);
        });
        List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_SUBMIT_ERROR)
                .timeoutSeconds(4)
                .tasks(suppliers)
                .build()
                .submit();
        //
        Assertions.assertEquals("first supplier", results.get(0).getContent());
        //这里不会取消，因为countDownLunch会等运行完成才减1。
        Assertions.assertEquals("second supplier", results.get(1).getContent());
        Assertions.assertEquals("third supplier", results.get(2).getContent());
        System.out.println(results.get(3).getException().getClass().getName());
        Assertions.assertTrue(results.get(3).getException() instanceof RuntimeException);
        assertEquals(results.get(3).getException().getMessage(), errMessage);

        //测试RETURN_ON_TASK_ERROR
        //任务3在1秒后报错，任务0与2无需等待已经执行完成，任务1需3秒，将被取消
        results = TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_TASK_ERROR)
                .timeoutSeconds(4)
                .tasks(suppliers).build()
                .submit();
        //
        Assertions.assertEquals("first supplier", results.get(0).getContent());
        //
        Assertions.assertEquals(null, results.get(1).getContent());
        //这个执行时间最短
        Assertions.assertEquals("third supplier", results.get(2).getContent());

        Assertions.assertTrue(results.get(3).getException() instanceof RuntimeException);
        assertEquals(results.get(3).getException().getMessage(), errMessage);
    }


    @Test
    void test_RETURN_ON_TASK_ERROR() {
        String errMessage = "这里有运行时异常！！";
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> "first supplier");
        suppliers.add(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException();
            }
            return "second supplier";
        });
        suppliers.add(() -> "third supplier");
        suppliers.add(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException(errMessage);
            }
            throw new RuntimeException(errMessage);
        });

        //测试RETURN_ON_TASK_ERROR
        //任务3在1秒后报错，任务0与2无需等待已经执行完成，任务1需3秒，将被取消
        List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_TASK_ERROR)
                .timeoutSeconds(4)
                .tasks(suppliers).build()
                .submit();
        //
        Assertions.assertEquals("first supplier", results.get(0).getContent());
        //
        Assertions.assertEquals(null, results.get(1).getContent());
        //这个执行时间最短
        Assertions.assertEquals("third supplier", results.get(2).getContent());

        Assertions.assertTrue(results.get(3).getException() instanceof RuntimeException);
        assertEquals(results.get(3).getException().getMessage(), errMessage);
    }


    @Test
    void testTaskNoTimeout() {
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> "first supplier");
        suppliers.add(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "second supplier";
        });
        suppliers.add(() -> "third supplier");
        String errMessage = "这里有运行时异常！！";
        suppliers.add(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(errMessage);
        });
        List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_SUBMIT_ERROR)
                .tasks(suppliers)
                .timeoutSeconds(-1)
                .build()
                .submit();
        //
        Assertions.assertEquals("first supplier", results.get(0).getContent());
        Assertions.assertEquals("second supplier", results.get(1).getContent());
        Assertions.assertEquals("third supplier", results.get(2).getContent());
        System.out.println(results.get(3).getException().getClass().getName());
        Assertions.assertTrue(results.get(3).getException() instanceof RuntimeException);
        assertEquals(results.get(3).getException().getMessage(), errMessage);
    }

    @Test
    void testTaskTimeout() {
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> "first supplier");
        suppliers.add(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException();
            }
            return "second supplier";
        });
        suppliers.add(() -> "third supplier");
//        String errMessage = "这里有运行时异常！！";
        suppliers.add(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "fourth supplier";
        });
        List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_SUBMIT_ERROR)
                .tasks(suppliers)
                .timeoutSeconds(2)
                .build()
                .submit();
        //
        Assertions.assertEquals("first supplier", results.get(0).getContent());
        Assertions.assertNull(results.get(1).getContent());
        Assertions.assertEquals("third supplier", results.get(2).getContent());
        Assertions.assertEquals("fourth supplier", results.get(3).getContent());
    }

    @Test
    void testTaskSubmitForPolicyContinueOnError() {
        long taskSleepTime = 500L;
        long start = System.currentTimeMillis();
        List<Supplier<?>> suppliers = new ArrayList<>();
        // 0
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "first supplier";
        });
        // 1
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "second supplier";
        });
        // 2
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "third supplier";
        });
        // 3
        String errMessage = "这里有运行时异常！！";
        suppliers.add(() -> {
            throw new RuntimeException(errMessage);
        });
        List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.singleThreadExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.CONTINUE_ON_ERROR)
                .tasks(suppliers)
                .build()
                .submit();

        Assertions.assertEquals("first supplier", results.get(0).getContent());
        Assertions.assertEquals("second supplier", results.get(1).getContent());
        // 由于只一个线程及一个队列，这个被拒绝
        Assertions.assertNull(results.get(2).getContent());
        // 这个也被被拒绝
        Assertions.assertTrue(results.get(3).getException() instanceof RuntimeException);
        Assertions.assertTrue(results.get(3).getException().getMessage().contains("由于没有线程资源进行处理，已经被拒绝！"));
        // 单线程运行，时间较长。
        Assertions.assertTrue(System.currentTimeMillis() - start < taskSleepTime * 3);
    }

    @Test
    void test_RETURN_ON_SUBMIT_ERROR() {
        long taskSleepTime = 3_000L;
        long start = System.currentTimeMillis();
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("first error");
            }
            return "first supplier";
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "second supplier";
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "third supplier";
        });
        String errMessage = "这里有运行时异常！！";
        suppliers.add(() -> {
            throw new RuntimeException(errMessage);
        });
        List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.singleThreadExecutorService)
                .errorPolicy(TaskExecuteErrorPolicy.RETURN_ON_SUBMIT_ERROR)
                .timeoutSeconds(4)
                .tasks(suppliers).build()
                .submit();
        Assertions.assertNull(results.get(0).getContent());
        Assertions.assertNull(results.get(1).getContent());
        //这个任务会被拒绝，导致所有未执行的任务都会返回为空。所以结果不用等4秒。
        Assertions.assertNull(results.get(2).getContent());
        Assertions.assertTrue(results.get(1).getException().getMessage().contains("由于没有线程资源进行处理，已经被拒绝！") || results.get(2).getException().getMessage().contains("由于没有线程资源进行处理，已经被拒绝！"));
        //测试提交拒绝时，任务会中断。所花费时间小于
        assertTrue(System.currentTimeMillis() - start < taskSleepTime);
    }

    @Test
    void test_THROW_TIMEOUT_ERROR() {
        long taskSleepTime = 3_000L;
        long start = System.currentTimeMillis();
        List<Supplier<?>> suppliers = new ArrayList<>();
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("first error");
            }
            return "first supplier";
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "second supplier";
        });
        suppliers.add(() -> {
            try {
                Thread.sleep(taskSleepTime);
            } catch (InterruptedException e) {
                log.error("[testSpeedTime][error][" + e.getMessage() + "]", e);
                Thread.currentThread().interrupt();
            }
            return "third supplier";
        });
        String errMessage = "这里有运行时异常！！";
        suppliers.add(() -> {
            throw new RuntimeException(errMessage);
        });
        try {
            List<TasksExecute.Result> results = TasksExecute.builder().executorService(ThreadPool.ioExecutorService)
                    .errorPolicy(TaskExecuteErrorPolicy.CONTINUE_ON_ERROR)
                    .timeoutSeconds(2)
                    .timeOutPolicy(TaskExecuteTimeOutPolicy.THROW_TIMEOUT_ERROR)
                    .tasks(suppliers).build()
                    .submit();
            Assertions.assertNull(results.get(0).getContent());
            Assertions.assertNull(results.get(1).getContent());
            //这个任务会被拒绝，导致所有未执行的任务都会返回为空。所以结果不用等4秒。
            Assertions.assertNull(results.get(2).getContent());
            Assertions.assertTrue(results.get(1).getException().getMessage().contains("由于没有线程资源进行处理，已经被拒绝！") || results.get(2).getException().getMessage().contains("由于没有线程资源进行处理，已经被拒绝！"));
            //测试提交拒绝时，任务会中断。所花费时间小于
            assertTrue(System.currentTimeMillis() - start < taskSleepTime);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof CodedException);
            Assertions.assertTrue(e.getMessage().contains("超时！2秒未处理完成"));
        }


    }

}
