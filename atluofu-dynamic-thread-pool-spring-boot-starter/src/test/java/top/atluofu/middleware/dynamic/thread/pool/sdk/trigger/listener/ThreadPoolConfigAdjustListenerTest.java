package top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.listener;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @ClassName: ThreadPoolConfigAdjustListenerTest
 * @description: 线程池配置监听器单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
public class ThreadPoolConfigAdjustListenerTest {

    private IDynamicThreadPoolService mockThreadPoolService;
    private IRegistry mockRegistry;
    private ThreadPoolConfigAdjustListener listener;

    @Before
    public void setUp() {
        mockThreadPoolService = mock(IDynamicThreadPoolService.class);
        mockRegistry = mock(IRegistry.class);
        listener = new ThreadPoolConfigAdjustListener(mockThreadPoolService, mockRegistry);
    }

    /**
     * 测试 1：监听器收到消息后调整配置
     */
    @Test
    public void test_onMessage_AdjustConfig() {
        // 准备测试数据
        ThreadPoolConfigEntity configEntity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        configEntity.setCorePoolSize(20);
        configEntity.setMaximumPoolSize(50);

        // 模拟服务返回
        List<ThreadPoolConfigEntity> mockList = new ArrayList<>();
        mockList.add(configEntity);
        when(mockThreadPoolService.queryThreadPoolList()).thenReturn(mockList);
        when(mockThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01")).thenReturn(configEntity);

        // 执行监听
        listener.onMessage("test-channel", configEntity);

        // 验证服务方法被调用
        ArgumentCaptor<ThreadPoolConfigEntity> captor = ArgumentCaptor.forClass(ThreadPoolConfigEntity.class);
        verify(mockThreadPoolService, times(1)).updateThreadPoolConfig(captor.capture());

        ThreadPoolConfigEntity capturedConfig = captor.getValue();
        assertEquals("核心线程数应该正确", 20, capturedConfig.getCorePoolSize());
        assertEquals("最大线程数应该正确", 50, capturedConfig.getMaximumPoolSize());
    }

    /**
     * 测试 2：监听器收到消息后上报数据
     */
    @Test
    public void test_onMessage_ReportData() {
        ThreadPoolConfigEntity configEntity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        configEntity.setCorePoolSize(20);
        configEntity.setMaximumPoolSize(50);

        List<ThreadPoolConfigEntity> mockList = new ArrayList<>();
        mockList.add(configEntity);
        when(mockThreadPoolService.queryThreadPoolList()).thenReturn(mockList);
        when(mockThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01")).thenReturn(configEntity);

        listener.onMessage("test-channel", configEntity);

        // 验证上报方法被调用
        verify(mockRegistry, times(1)).reportThreadPool(mockList);
        verify(mockRegistry, times(1)).reportThreadPoolConfigParameter(configEntity);
    }

    /**
     * 测试 3：验证日志字段使用正确的 corePoolSize
     */
    @Test
    public void test_onMessage_LogFieldCorrect() {
        ThreadPoolConfigEntity configEntity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        configEntity.setCorePoolSize(30);  // 设置核心线程数
        configEntity.setMaximumPoolSize(60);
        configEntity.setPoolSize(10);  // 设置池中线程数（不应该用于日志）

        List<ThreadPoolConfigEntity> mockList = new ArrayList<>();
        mockList.add(configEntity);
        when(mockThreadPoolService.queryThreadPoolList()).thenReturn(mockList);
        when(mockThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01")).thenReturn(configEntity);

        // 执行时不应该抛出异常，日志应该使用 corePoolSize 而不是 poolSize
        listener.onMessage("test-channel", configEntity);

        // 验证 updateThreadPoolConfig 被调用时使用的是 corePoolSize
        ArgumentCaptor<ThreadPoolConfigEntity> captor = ArgumentCaptor.forClass(ThreadPoolConfigEntity.class);
        verify(mockThreadPoolService).updateThreadPoolConfig(captor.capture());
        assertEquals("应该使用 corePoolSize", 30, captor.getValue().getCorePoolSize());
    }

    /**
     * 测试 4：验证上报时使用查询到的最新数据
     */
    @Test
    public void test_onMessage_ReportCurrentData() {
        ThreadPoolConfigEntity requestConfig = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        requestConfig.setCorePoolSize(30);
        requestConfig.setMaximumPoolSize(60);

        // 模拟查询返回的实际数据（可能包含更多状态信息）
        ThreadPoolConfigEntity currentConfig = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        currentConfig.setCorePoolSize(30);
        currentConfig.setMaximumPoolSize(60);
        currentConfig.setActiveCount(5);
        currentConfig.setQueueSize(10);

        List<ThreadPoolConfigEntity> mockList = new ArrayList<>();
        mockList.add(currentConfig);
        when(mockThreadPoolService.queryThreadPoolList()).thenReturn(mockList);
        when(mockThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01")).thenReturn(currentConfig);

        listener.onMessage("test-channel", requestConfig);

        // 验证上报的是查询到的最新数据，而不是请求数据
        ArgumentCaptor<ThreadPoolConfigEntity> reportCaptor = ArgumentCaptor.forClass(ThreadPoolConfigEntity.class);
        verify(mockRegistry).reportThreadPoolConfigParameter(reportCaptor.capture());

        ThreadPoolConfigEntity reportedConfig = reportCaptor.getValue();
        assertEquals("活跃线程数应该被上报", 5, reportedConfig.getActiveCount());
        assertEquals("队列大小应该被上报", 10, reportedConfig.getQueueSize());
    }
}
