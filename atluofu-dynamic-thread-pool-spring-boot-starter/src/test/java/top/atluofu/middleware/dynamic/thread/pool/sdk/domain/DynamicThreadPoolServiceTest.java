package top.atluofu.middleware.dynamic.thread.pool.sdk.domain;

import org.junit.jupiter.api.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutorRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter.ThreadPoolExecutorManagedExecutor;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @ClassName: DynamicThreadPoolServiceTest
 * @description: 动态线程池服务单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
public class DynamicThreadPoolServiceTest {

    @Test
    public void test_queryExecutorSnapshots() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );

        List<ExecutorSnapshot> snapshots = service.queryExecutorSnapshots();

        assertEquals(1, snapshots.size());
        assertEquals("orderExecutor", snapshots.get(0).getExecutorName());
        assertEquals("app", snapshots.get(0).getAppName());
        assertEquals("instance", snapshots.get(0).getInstanceId());
    }

    @Test
    public void test_queryExecutorSnapshot() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );

        ExecutorSnapshot snapshot = service.queryExecutorSnapshot("orderExecutor");

        assertNotNull(snapshot);
        assertEquals("orderExecutor", snapshot.getExecutorName());
        assertNull(service.queryExecutorSnapshot("missingExecutor"));
    }

    @Test
    public void test_updateExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutor managedExecutor = new ThreadPoolExecutorManagedExecutor(
                "app", "instance", "orderExecutor", executor
        );
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of(managedExecutor))
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setExecutorName("orderExecutor");
        command.setCorePoolSize(3);
        command.setMaximumPoolSize(9);

        UpdateResult result = service.updateExecutor(command);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getMessage());
        assertEquals(3, executor.getCorePoolSize());
        assertEquals(9, executor.getMaximumPoolSize());
        assertEquals(2, result.getBefore().getCorePoolSize());
        assertEquals(3, result.getAfter().getCorePoolSize());
    }

    @Test
    public void test_updateExecutorRejectsBlankName() {
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of())
        );

        UpdateResult result = service.updateExecutor(new ExecutorUpdateCommand());

        assertFalse(result.isSuccess());
        assertEquals("executorName must not be blank", result.getMessage());
    }

    @Test
    public void test_updateExecutorRejectsMissingExecutor() {
        DynamicThreadPoolService service = new DynamicThreadPoolService(
                new ManagedExecutorRegistry(List.of())
        );
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setExecutorName("missingExecutor");

        UpdateResult result = service.updateExecutor(command);

        assertFalse(result.isSuccess());
        assertEquals("executor not found: missingExecutor", result.getMessage());
    }

}
