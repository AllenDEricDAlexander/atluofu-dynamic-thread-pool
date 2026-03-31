package top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.job;

import org.junit.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @description ThreadPoolDataReportJob 单元测试
 */
public class ThreadPoolDataReportJobTest {

    /**
     * 测试：正常上报线程池列表和配置
     */
    @Test
    public void test_execReportThreadPoolList_Success() {
        IDynamicThreadPoolService mockDynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        IRegistry mockRegistry = mock(IRegistry.class);
        ThreadPoolDataReportJob job = new ThreadPoolDataReportJob("test-app", mockDynamicThreadPoolService, mockRegistry);

        ThreadPoolConfigEntity e1 = new ThreadPoolConfigEntity("test-app", "pool1");
        ThreadPoolConfigEntity e2 = new ThreadPoolConfigEntity("test-app", "pool2");
        List<ThreadPoolConfigEntity> list = Arrays.asList(e1, e2);

        when(mockDynamicThreadPoolService.queryThreadPoolList()).thenReturn(list);

        job.execReportThreadPoolList();

        verify(mockDynamicThreadPoolService, times(1)).queryThreadPoolList();
        verify(mockRegistry, times(1)).reportThreadPoolByApp(eq("test-app"), eq(list));
        verify(mockRegistry, times(1)).reportThreadPoolConfigParameter(e1);
        verify(mockRegistry, times(1)).reportThreadPoolConfigParameter(e2);
    }

    /**
     * 测试：查询线程池列表抛异常时，方法应吞掉异常不向外抛
     */
    @Test
    public void test_execReportThreadPoolList_QueryException() {
        IDynamicThreadPoolService mockDynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        IRegistry mockRegistry = mock(IRegistry.class);
        ThreadPoolDataReportJob job = new ThreadPoolDataReportJob("test-app", mockDynamicThreadPoolService, mockRegistry);

        when(mockDynamicThreadPoolService.queryThreadPoolList())
                .thenThrow(new RuntimeException("query error"));

        job.execReportThreadPoolList();

        verify(mockDynamicThreadPoolService, times(1)).queryThreadPoolList();
        verifyNoInteractions(mockRegistry);
    }

    /**
     * 测试：单个线程池上报失败不影响其他
     */
    @Test
    public void test_execReportThreadPoolList_SingleReportFail() {
        IDynamicThreadPoolService mockDynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        IRegistry mockRegistry = mock(IRegistry.class);
        ThreadPoolDataReportJob job = new ThreadPoolDataReportJob("test-app", mockDynamicThreadPoolService, mockRegistry);

        ThreadPoolConfigEntity e1 = new ThreadPoolConfigEntity("test-app", "pool1");
        ThreadPoolConfigEntity e2 = new ThreadPoolConfigEntity("test-app", "pool2");
        List<ThreadPoolConfigEntity> list = Arrays.asList(e1, e2);

        when(mockDynamicThreadPoolService.queryThreadPoolList()).thenReturn(list);
        doThrow(new RuntimeException("report error"))
                .when(mockRegistry).reportThreadPoolConfigParameter(e1);

        job.execReportThreadPoolList();

        verify(mockRegistry, times(1)).reportThreadPoolByApp(eq("test-app"), eq(list));
        verify(mockRegistry, times(1)).reportThreadPoolConfigParameter(e1);
        verify(mockRegistry, times(1)).reportThreadPoolConfigParameter(e2);
    }
}

