package top.atluofu.middleware.dynamic.thread.pool.sdk.domain;

import org.junit.Before;
import org.junit.Test;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @ClassName: DynamicThreadPoolServiceTest
 * @description: 动态线程池服务单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
public class DynamicThreadPoolServiceTest {

    private DynamicThreadPoolService dynamicThreadPoolService;

    private final String applicationName = "test-app";

    private Map<String, ThreadPoolExecutor> threadPoolExecutorMap;

    @Before
    public void setUp() {
        threadPoolExecutorMap = new HashMap<>();
        
        // 创建测试线程池 1
        ThreadPoolExecutor executor1 = new ThreadPoolExecutor(
                10,  // corePoolSize
                50,  // maximumPoolSize
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        threadPoolExecutorMap.put("threadPoolExecutor01", executor1);

        // 创建测试线程池 2
        ThreadPoolExecutor executor2 = new ThreadPoolExecutor(
                20,  // corePoolSize
                80,  // maximumPoolSize
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200)
        );
        threadPoolExecutorMap.put("threadPoolExecutor02", executor2);

        dynamicThreadPoolService = new DynamicThreadPoolService(applicationName, threadPoolExecutorMap);
    }

    /**
     * 测试 1：查询线程池列表
     */
    @Test
    public void test_queryThreadPoolList() {
        List<ThreadPoolConfigEntity> threadPoolList = dynamicThreadPoolService.queryThreadPoolList();
        
        assertNotNull("线程池列表不应为空", threadPoolList);
        assertEquals("应该返回 2 个线程池", 2, threadPoolList.size());
        
        // 验证线程池 1 的配置
        ThreadPoolConfigEntity entity1 = threadPoolList.stream()
                .filter(e -> "threadPoolExecutor01".equals(e.getThreadPoolName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull("应该找到 threadPoolExecutor01", entity1);
        assertEquals("应用名应该正确", applicationName, entity1.getAppName());
        assertEquals("核心线程数应该为 10", 10, entity1.getCorePoolSize());
        assertEquals("最大线程数应该为 50", 50, entity1.getMaximumPoolSize());
        assertEquals("队列类型应该是 LinkedBlockingQueue", "LinkedBlockingQueue", entity1.getQueueType());
        
        // 验证线程池 2 的配置
        ThreadPoolConfigEntity entity2 = threadPoolList.stream()
                .filter(e -> "threadPoolExecutor02".equals(e.getThreadPoolName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull("应该找到 threadPoolExecutor02", entity2);
        assertEquals("核心线程数应该为 20", 20, entity2.getCorePoolSize());
        assertEquals("最大线程数应该为 80", 80, entity2.getMaximumPoolSize());
    }

    /**
     * 测试 2：根据名称查询线程池配置
     */
    @Test
    public void test_queryThreadPoolConfigByName() {
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        
        assertNotNull("查询结果不应为空", entity);
        assertEquals("应用名应该正确", applicationName, entity.getAppName());
        assertEquals("线程池名称应该正确", "threadPoolExecutor01", entity.getThreadPoolName());
        assertEquals("核心线程数应该为 10", 10, entity.getCorePoolSize());
        assertEquals("最大线程数应该为 50", 50, entity.getMaximumPoolSize());
    }

    /**
     * 测试 3：查询不存在的线程池
     */
    @Test
    public void test_queryNonExistentThreadPool() {
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("nonExistentPool");
        
        assertNotNull("即使线程池不存在，也应返回实体对象", entity);
        assertEquals("应用名应该正确", applicationName, entity.getAppName());
        assertEquals("线程池名称应该正确", "nonExistentPool", entity.getThreadPoolName());
        assertEquals("核心线程数应该为 0", 0, entity.getCorePoolSize());
        assertEquals("最大线程数应该为 0", 0, entity.getMaximumPoolSize());
    }

    /**
     * 测试 4：动态调整线程池配置
     */
    @Test
    public void test_updateThreadPoolConfig() {
        // 调整前的配置
        ThreadPoolConfigEntity beforeConfig = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertEquals("调整前核心线程数应为 10", 10, beforeConfig.getCorePoolSize());
        assertEquals("调整前最大线程数应为 50", 50, beforeConfig.getMaximumPoolSize());

        // 创建新的配置
        ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity(applicationName, "threadPoolExecutor01");
        newConfig.setCorePoolSize(30);
        newConfig.setMaximumPoolSize(100);

        // 执行调整
        dynamicThreadPoolService.updateThreadPoolConfig(newConfig);

        // 验证调整后的配置
        ThreadPoolConfigEntity afterConfig = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertEquals("调整后核心线程数应为 30", 30, afterConfig.getCorePoolSize());
        assertEquals("调整后最大线程数应为 100", 100, afterConfig.getMaximumPoolSize());
    }

    /**
     * 测试 5：调整不存在的线程池（应该静默失败）
     */
    @Test
    public void test_updateNonExistentThreadPoolConfig() {
        ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity(applicationName, "nonExistentPool");
        newConfig.setCorePoolSize(30);
        newConfig.setMaximumPoolSize(100);

        // 不应该抛出异常
        dynamicThreadPoolService.updateThreadPoolConfig(newConfig);
        
        // 验证没有影响其他线程池
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertEquals("核心线程数应保持不变", 10, entity.getCorePoolSize());
    }

    /**
     * 测试 6：应用名不匹配时不应该调整
     */
    @Test
    public void test_updateConfigWithWrongAppName() {
        ThreadPoolConfigEntity wrongConfig = new ThreadPoolConfigEntity("wrong-app-name", "threadPoolExecutor01");
        wrongConfig.setCorePoolSize(30);
        wrongConfig.setMaximumPoolSize(100);

        // 不应该调整
        dynamicThreadPoolService.updateThreadPoolConfig(wrongConfig);

        // 验证配置没有变化
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertEquals("核心线程数应保持不变", 10, entity.getCorePoolSize());
        assertEquals("最大线程数应保持不变", 50, entity.getMaximumPoolSize());
    }

    /**
     * 测试 7：验证线程池状态字段（活跃线程数、队列大小等）
     */
    @Test
    public void test_threadPoolStatusFields() throws InterruptedException {
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        
        // 初始状态
        assertEquals("初始活跃线程数应为 0", 0, entity.getActiveCount());
        assertEquals("初始队列大小应为 0", 0, entity.getQueueSize());
        
        // 提交一些任务
        ThreadPoolExecutor executor = threadPoolExecutorMap.get("threadPoolExecutor01");
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 等待任务执行
        Thread.sleep(50);
        
        entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertTrue("应该有活跃线程", entity.getActiveCount() > 0);
        
        // 等待任务完成
        Thread.sleep(200);
        
        entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertEquals("任务完成后活跃线程数应为 0", 0, entity.getActiveCount());
    }

    /**
     * 测试 8：验证 null 配置处理
     */
    @Test
    public void test_updateNullConfig() {
        // 不应该抛出异常
        dynamicThreadPoolService.updateThreadPoolConfig(null);
        
        // 验证配置没有变化
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        assertEquals("核心线程数应保持不变", 10, entity.getCorePoolSize());
    }
}
