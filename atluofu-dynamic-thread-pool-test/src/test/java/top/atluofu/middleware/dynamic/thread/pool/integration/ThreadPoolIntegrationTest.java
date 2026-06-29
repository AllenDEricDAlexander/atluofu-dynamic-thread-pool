package top.atluofu.middleware.dynamic.thread.pool.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: ThreadPoolIntegrationTest
 * @description: 线程池集成测试 - 需要 Redis 环境
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@Slf4j
@SpringBootTest
public class ThreadPoolIntegrationTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    /**
     * 测试 1：查询线程池列表集成测试
     */
    @Test
    public void test_queryThreadPoolList_Integration() {
        log.info("========== 集成测试：查询线程池列表 ==========");
        
        List<ThreadPoolConfigEntity> threadPoolList = dynamicThreadPoolService.queryThreadPoolList();
        
        assertNotNull(threadPoolList, "线程池列表不应为空");
        log.info("查询到 {} 个线程池", threadPoolList.size());
        
        for (ThreadPoolConfigEntity entity : threadPoolList) {
            log.info("线程池：{} - 核心：{}, 最大：{}, 活跃：{}, 队列：{}", 
                    entity.getThreadPoolName(),
                    entity.getCorePoolSize(),
                    entity.getMaximumPoolSize(),
                    entity.getActiveCount(),
                    entity.getQueueSize());
            
            assertNotNull(entity.getAppName(), "应用名不应为空");
            assertNotNull(entity.getThreadPoolName(), "线程池名称不应为空");
            assertTrue(entity.getCorePoolSize() > 0, "核心线程数应该大于 0");
            assertTrue(entity.getMaximumPoolSize() > 0, "最大线程数应该大于 0");
        }
    }

    /**
     * 测试 2：动态调整线程池配置集成测试
     */
    @Test
    public void test_adjustThreadPoolConfig_Integration() throws InterruptedException {
        log.info("========== 集成测试：动态调整线程池配置 ==========");
        
        String threadPoolName = "threadPoolExecutor01";
        
        // 获取调整前的配置
        ThreadPoolConfigEntity beforeConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
        log.info("调整前 - 核心线程数：{}, 最大线程数：{}", 
                beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());
        
        int originalCoreSize = beforeConfig.getCorePoolSize();
        int originalMaxSize = beforeConfig.getMaximumPoolSize();
        
        try {
            // 发布新的配置
            ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity(
                    "dynamic-thread-pool-test-app", threadPoolName);
            newConfig.setCorePoolSize(originalCoreSize + 5);
            newConfig.setMaximumPoolSize(originalMaxSize + 10);
            
            log.info("发布新配置 - 核心线程数：{}, 最大线程数：{}", 
                    newConfig.getCorePoolSize(), newConfig.getMaximumPoolSize());
            
            dynamicThreadPoolRedisTopic.publish(newConfig);
            
            // 等待配置生效
            Thread.sleep(2000);
            
            // 验证调整后的配置
            ThreadPoolConfigEntity afterConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
            log.info("调整后 - 核心线程数：{}, 最大线程数：{}", 
                    afterConfig.getCorePoolSize(), afterConfig.getMaximumPoolSize());
            
            assertEquals(originalCoreSize + 5, afterConfig.getCorePoolSize(), "核心线程数应该被调整");
            assertEquals(originalMaxSize + 10, afterConfig.getMaximumPoolSize(), "最大线程数应该被调整");
            
            log.info("✓ 动态调整成功！");
            
        } finally {
            // 恢复原始配置
            ThreadPoolConfigEntity restoreConfig = new ThreadPoolConfigEntity(
                    "dynamic-thread-pool-test-app", threadPoolName);
            restoreConfig.setCorePoolSize(originalCoreSize);
            restoreConfig.setMaximumPoolSize(originalMaxSize);
            dynamicThreadPoolRedisTopic.publish(restoreConfig);
            Thread.sleep(1000);
            
            log.info("已恢复原始配置");
        }
    }

    /**
     * 测试 3：线程池状态实时监控集成测试
     */
    @Test
    public void test_threadPoolStatusMonitor_Integration() throws InterruptedException {
        log.info("========== 集成测试：线程池状态实时监控 ==========");
        
        String threadPoolName = "threadPoolExecutor01";
        
        // 初始状态
        ThreadPoolConfigEntity initialConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
        log.info("初始状态 - 活跃线程：{}, 队列任务：{}, 剩余容量：{}", 
                initialConfig.getActiveCount(), 
                initialConfig.getQueueSize(), 
                initialConfig.getRemainingCapacity());
        
        // 验证状态字段
        assertTrue(initialConfig.getActiveCount() >= 0, "活跃线程数应该大于等于 0");
        assertTrue(initialConfig.getQueueSize() >= 0, "队列任务数应该大于等于 0");
        assertTrue(initialConfig.getRemainingCapacity() >= 0, "剩余容量应该大于等于 0");
        assertNotNull(initialConfig.getQueueType(), "队列类型不应为空");
        
        log.info("✓ 状态监控正常！");
    }

    /**
     * 测试 4：配置变更监听器集成测试
     */
    @Test
    public void test_configAdjustListener_Integration() throws InterruptedException {
        log.info("========== 集成测试：配置变更监听器 ==========");
        
        String threadPoolName = "threadPoolExecutor02";
        
        // 获取原始配置
        ThreadPoolConfigEntity originalConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
        log.info("原始配置 - 核心：{}, 最大：{}", 
                originalConfig.getCorePoolSize(), originalConfig.getMaximumPoolSize());
        
        int originalCoreSize = originalConfig.getCorePoolSize();
        
        try {
            // 发布配置变更
            ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity(
                    "dynamic-thread-pool-test-app", threadPoolName);
            newConfig.setCorePoolSize(25);
            newConfig.setMaximumPoolSize(75);
            
            log.info("发布配置变更");
            dynamicThreadPoolRedisTopic.publish(newConfig);
            
            // 等待监听器处理
            Thread.sleep(3000);
            
            // 验证配置已更新
            ThreadPoolConfigEntity updatedConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
            log.info("更新后配置 - 核心：{}, 最大：{}", 
                    updatedConfig.getCorePoolSize(), updatedConfig.getMaximumPoolSize());
            
            assertEquals(25, updatedConfig.getCorePoolSize(), "核心线程数应该被更新");
            assertEquals(75, updatedConfig.getMaximumPoolSize(), "最大线程数应该被更新");
            
            log.info("✓ 监听器工作正常！");
            
        } finally {
            // 恢复原始配置
            ThreadPoolConfigEntity restoreConfig = new ThreadPoolConfigEntity(
                    "dynamic-thread-pool-test-app", threadPoolName);
            restoreConfig.setCorePoolSize(originalCoreSize);
            dynamicThreadPoolRedisTopic.publish(restoreConfig);
            Thread.sleep(1000);
        }
    }

    /**
     * 测试 5：并发配置更新测试
     */
    @Test
    public void test_concurrentConfigUpdate_Integration() throws InterruptedException {
        log.info("========== 集成测试：并发配置更新 ==========");
        
        String threadPoolName = "threadPoolExecutor01";
        CountDownLatch latch = new CountDownLatch(3);
        
        // 并发发布 3 次配置更新
        for (int i = 0; i < 3; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    ThreadPoolConfigEntity config = new ThreadPoolConfigEntity(
                            "dynamic-thread-pool-test-app", threadPoolName);
                    config.setCorePoolSize(10 + index * 5);
                    config.setMaximumPoolSize(50 + index * 10);
                    
                    dynamicThreadPoolRedisTopic.publish(config);
                    log.info("发布配置 #{}: 核心={}, 最大={}", 
                            index, config.getCorePoolSize(), config.getMaximumPoolSize());
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // 等待所有更新完成
        latch.await(10, TimeUnit.SECONDS);
        Thread.sleep(2000);
        
        // 验证最终配置
        ThreadPoolConfigEntity finalConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
        log.info("最终配置 - 核心：{}, 最大：{}", 
                finalConfig.getCorePoolSize(), finalConfig.getMaximumPoolSize());
        
        assertNotNull(finalConfig, "最终配置不应为空");
        log.info("✓ 并发更新处理完成！");
    }
}
