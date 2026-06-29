package top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.job;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @description ThreadPoolDataReportJob 单元测试
 */
public class ThreadPoolDataReportJobTest {

    @Test
    public void test_execReportThreadPoolList_success() {
        IDynamicThreadPoolService dynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        IRegistry registry = mock(IRegistry.class);
        ThreadPoolDataReportJob job = new ThreadPoolDataReportJob(dynamicThreadPoolService, registry);
        ExecutorSnapshot first = buildSnapshot("pool1");
        ExecutorSnapshot second = buildSnapshot("pool2");
        List<ExecutorSnapshot> snapshots = List.of(first, second);
        when(dynamicThreadPoolService.queryExecutorSnapshots()).thenReturn(snapshots);

        job.execReportThreadPoolList();

        verify(dynamicThreadPoolService, times(1)).queryExecutorSnapshots();
        verify(registry, times(1)).reportSnapshots(snapshots);
    }

    @Test
    public void test_execReportThreadPoolList_queryException() {
        IDynamicThreadPoolService dynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        IRegistry registry = mock(IRegistry.class);
        ThreadPoolDataReportJob job = new ThreadPoolDataReportJob(dynamicThreadPoolService, registry);
        when(dynamicThreadPoolService.queryExecutorSnapshots()).thenThrow(new RuntimeException("query error"));

        job.execReportThreadPoolList();

        verify(dynamicThreadPoolService, times(1)).queryExecutorSnapshots();
        verifyNoInteractions(registry);
    }

    @Test
    public void test_execReportThreadPoolList_shouldUseReportIntervalProperty() throws NoSuchMethodException {
        Method method = ThreadPoolDataReportJob.class.getMethod("execReportThreadPoolList");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString()).isEqualTo("${atluofu.dynamic.thread-pool.report.interval:20s}");
    }

    private ExecutorSnapshot buildSnapshot(String executorName) {
        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName("app");
        snapshot.setInstanceId("instance");
        snapshot.setExecutorName(executorName);
        return snapshot;
    }

}
