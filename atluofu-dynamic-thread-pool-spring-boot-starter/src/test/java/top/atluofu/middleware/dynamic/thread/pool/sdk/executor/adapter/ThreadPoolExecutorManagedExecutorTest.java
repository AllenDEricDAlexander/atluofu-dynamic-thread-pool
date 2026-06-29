package top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter;

import org.junit.jupiter.api.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ClassName: ThreadPoolExecutorManagedExecutorTest
 * @description: 平台线程池托管执行器测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
public class ThreadPoolExecutorManagedExecutorTest {

    @Test
    public void test_snapshotFields() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );

        ExecutorSnapshot snapshot = managedExecutor.snapshot();

        assertEquals("test-app", snapshot.getAppName());
        assertEquals("instance-01", snapshot.getInstanceId());
        assertEquals("testExecutor", snapshot.getExecutorName());
        assertEquals(ExecutorKind.PLATFORM_THREAD_POOL, snapshot.getExecutorKind());
        assertFalse(snapshot.isVirtual());
        assertTrue(snapshot.isResizable());
        assertEquals(5, snapshot.getCorePoolSize());
        assertEquals(10, snapshot.getMaximumPoolSize());
        assertEquals(0, snapshot.getActiveCount());
        assertEquals(0, snapshot.getPoolSize());
        assertEquals(0L, snapshot.getTaskCount());
        assertEquals(0L, snapshot.getCompletedTaskCount());
        assertEquals("LinkedBlockingQueue", snapshot.getQueueType());
        assertEquals(0, snapshot.getQueueSize());
        assertEquals(100, snapshot.getRemainingCapacity());
        assertNotNull(snapshot.getReportTime());
        assertTrue(managedExecutor.supportsResize());
        assertFalse(managedExecutor.supportsVirtualThread());
        assertTrue(managedExecutor.supportsQueueMetrics());
    }

    @Test
    public void test_updateSuccess() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ThreadPoolExecutorManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "test-app", "instance-01", "testExecutor", executor
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setCorePoolSize(12);
        command.setMaximumPoolSize(15);
        command.setKeepAliveSeconds(30L);
        command.setAllowCoreThreadTimeOut(true);

        UpdateResult result = managedExecutor.update(command);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getMessage());
        assertNotNull(result.getBefore());
        assertNotNull(result.getAfter());
        assertEquals(5, result.getBefore().getCorePoolSize());
        assertEquals(10, result.getBefore().getMaximumPoolSize());
        assertEquals(12, result.getAfter().getCorePoolSize());
        assertEquals(15, result.getAfter().getMaximumPoolSize());
        assertEquals(12, executor.getCorePoolSize());
        assertEquals(15, executor.getMaximumPoolSize());
        assertEquals(30L, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertTrue(executor.allowsCoreThreadTimeOut());
    }

}
