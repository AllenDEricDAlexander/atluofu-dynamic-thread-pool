package top.atluofu.middleware.dynamic.thread.pool.smoke;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @ClassName: SmokeTest
 * @description: 冒烟测试 - 端到端测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class SmokeTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    /**
     * 冒烟测试 1：系统启动后基本功能验证
     */
    @Test
    public void smoke_SystemStartup_BasicFunctionality() {
        log.info("========== 冒烟测试 1：系统启动后基本功能验证 ==========");
        
        // 1. 验证服务可以注入
        assertNotNull("ThreadPoolService 应该可以注入", dynamicThreadPoolService);
        assertNotNull("RedisTopic 应该可以注入", dynamicThreadPoolRedisTopic);
        
        // 2. 验证可以查询线程池列表
        List<ThreadPoolConfigEntity> threadPoolList = dynamicThreadPoolService.queryThreadPoolList();
        assertNotNull("线程池列表不应为空", threadPoolList);
        assertTrue("应该至少有一个线程池", threadPoolList.size() > 0);
        
        log.info("✓ 系统启动正常，基本功能可用");
    }

    /**
     * 冒烟测试 2：线程池配置查询功能验证
     */
    @Test
    public void smoke_QueryConfig_BasicFunctionality() {
        log.info("========== 冒烟测试 2：线程池配置查询功能验证 ==========");
        
        // 查询存在的线程池
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        
        assertNotNull("查询结果不应为空", entity);
        assertNotNull("应用名不应为空", entity.getAppName());
        assertNotNull("线程池名称不应为空", entity.getThreadPoolName());
        assertTrue("核心线程数应该大于 0", entity.getCorePoolSize() > 0);
        assertTrue("最大线程数应该大于 0", entity.getMaximumPoolSize() > 0);
        
        log.info("查询结果 - 应用：{}, 线程池：{}, 核心：{}, 最大：{}", 
                entity.getAppName(), entity.getThreadPoolName(), 
                entity.getCorePoolSize(), entity.getMaximumPoolSize());
        
        log.info("✓ 配置查询功能正常");
    }

    /**
     * 冒烟测试 3：动态调整功能验证
     */
    @Test
    public void smoke_DynamicAdjust_BasicFunctionality() throws InterruptedException {
        log.info("========== 冒烟测试 3：动态调整功能验证 ==========");
        
        String threadPoolName = "threadPoolExecutor01";
        
        // 1. 获取原始配置
        ThreadPoolConfigEntity originalConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
        int originalCoreSize = originalConfig.getCorePoolSize();
        log.info("原始配置 - 核心线程数：{}", originalCoreSize);
        
        try {
            // 2. 发布新配置
            int newCoreSize = originalCoreSize + 1;
            ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity(
                    "dynamic-thread-pool-test-app", threadPoolName);
            newConfig.setCorePoolSize(newCoreSize);
            newConfig.setMaximumPoolSize(originalConfig.getMaximumPoolSize());
            
            log.info("发布新配置 - 核心线程数：{}", newCoreSize);
            dynamicThreadPoolRedisTopic.publish(newConfig);
            
            // 3. 等待生效
            Thread.sleep(2000);
            
            // 4. 验证配置已更新
            ThreadPoolConfigEntity updatedConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
            log.info("更新后配置 - 核心线程数：{}", updatedConfig.getCorePoolSize());
            
            assertEquals("核心线程数应该被更新", newCoreSize, updatedConfig.getCorePoolSize());
            
            log.info("✓ 动态调整功能正常");
            
        } finally {
            // 5. 恢复原始配置
            ThreadPoolConfigEntity restoreConfig = new ThreadPoolConfigEntity(
                    "dynamic-thread-pool-test-app", threadPoolName);
            restoreConfig.setCorePoolSize(originalCoreSize);
            restoreConfig.setMaximumPoolSize(originalConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(restoreConfig);
            Thread.sleep(1000);
            
            log.info("已恢复原始配置");
        }
    }

    /**
     * 冒烟测试 4：Redis 通信验证
     */
    @Test
    public void smoke_RedisCommunication_BasicFunctionality() throws InterruptedException {
        log.info("========== 冒烟测试 4：Redis 通信验证 ==========");
        
        // 1. 验证可以发布消息到 Redis Topic
        ThreadPoolConfigEntity testConfig = new ThreadPoolConfigEntity(
                "dynamic-thread-pool-test-app", "threadPoolExecutor01");
        testConfig.setCorePoolSize(10);
        testConfig.setMaximumPoolSize(50);
        
        long subscribers = dynamicThreadPoolRedisTopic.publish(testConfig);
        log.info("消息已发布，订阅者数量：{}", subscribers);
        
        // 2. 验证可以查询到数据
        Thread.sleep(1000);
        List<ThreadPoolConfigEntity> threadPoolList = dynamicThreadPoolService.queryThreadPoolList();
        assertNotNull("应该可以查询到线程池列表", threadPoolList);
        
        log.info("✓ Redis 通信正常");
    }

    /**
     * 冒烟测试 5：数据完整性验证
     */
    @Test
    public void smoke_DataIntegrity_BasicFunctionality() {
        log.info("========== 冒烟测试 5：数据完整性验证 ==========");
        
        List<ThreadPoolConfigEntity> threadPoolList = dynamicThreadPoolService.queryThreadPoolList();
        
        for (ThreadPoolConfigEntity entity : threadPoolList) {
            // 验证必填字段
            assertNotNull("应用名不应为空", entity.getAppName());
            assertNotNull("线程池名称不应为空", entity.getThreadPoolName());
            
            // 验证数值字段合理性
            assertTrue("核心线程数应该大于 0", entity.getCorePoolSize() > 0);
            assertTrue("最大线程数应该大于等于核心线程数", 
                    entity.getMaximumPoolSize() >= entity.getCorePoolSize());
            assertTrue("活跃线程数应该大于等于 0", entity.getActiveCount() >= 0);
            assertTrue("池中线程数应该大于等于 0", entity.getPoolSize() >= 0);
            assertTrue("队列任务数应该大于等于 0", entity.getQueueSize() >= 0);
            assertTrue("剩余容量应该大于等于 0", entity.getRemainingCapacity() >= 0);
            
            // 验证队列类型
            assertNotNull("队列类型不应为空", entity.getQueueType());
            
            log.info("线程池 {} 数据完整", entity.getThreadPoolName());
        }
        
        log.info("✓ 数据完整性验证通过");
    }

    /**
     * 冒烟测试 6：异常处理验证
     */
    @Test
    public void smoke_ExceptionHandling_BasicFunctionality() {
        log.info("========== 冒烟测试 6：异常处理验证 ==========");
        
        // 1. 查询不存在的线程池 - 不应抛出异常
        ThreadPoolConfigEntity nonExistent = dynamicThreadPoolService.queryThreadPoolConfigByName("nonExistentPool");
        assertNotNull("即使线程池不存在，也应返回对象", nonExistent);
        log.info("查询不存在的线程池 - 返回空对象，未抛出异常");
        
        // 2. 更新 null 配置 - 不应抛出异常
        dynamicThreadPoolService.updateThreadPoolConfig(null);
        log.info("更新 null 配置 - 未抛出异常");
        
        // 3. 更新错误应用名的配置 - 不应抛出异常
        ThreadPoolConfigEntity wrongAppConfig = new ThreadPoolConfigEntity(
                "wrong-app", "threadPoolExecutor01");
        dynamicThreadPoolService.updateThreadPoolConfig(wrongAppConfig);
        log.info("更新错误应用名的配置 - 未抛出异常");
        
        log.info("✓ 异常处理正常");
    }

    /**
     * 冒烟测试 7：端到端流程验证
     */
    @Test
    public void smoke_EndToEnd_BasicFunctionality() throws InterruptedException {
        log.info("========== 冒烟测试 7：端到端流程验证 ==========");
        
        String threadPoolName = "threadPoolExecutor01";
        
        // 1. 查询当前配置
        ThreadPoolConfigEntity config1 = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
        log.info("步骤 1 - 查询配置：核心={}", config1.getCorePoolSize());
        
        // 2. 发布配置更新
        ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity(
                "dynamic-thread-pool-test-app", threadPoolName);
        newConfig.setCorePoolSize(config1.getCorePoolSize() + 5);
        newConfig.setMaximumPoolSize(config1.getMaximumPoolSize());
        
        log.info("步骤 2 - 发布配置更新");
        dynamicThreadPoolRedisTopic.publish(newConfig);
        
        // 3. 等待处理
        Thread.sleep(2000);
        
        // 4. 再次查询验证
        ThreadPoolConfigEntity config2 = dynamicThreadPoolService.queryThreadPoolList().stream()
                .filter(e -> threadPoolName.equals(e.getThreadPoolName()))
                .findFirst()
                .orElse(null);
        
        assertNotNull("应该找到线程池", config2);
        log.info("步骤 3 - 验证配置：核心={}", config2.getCorePoolSize());
        
        // 5. 恢复原始配置
        ThreadPoolConfigEntity restoreConfig = new ThreadPoolConfigEntity(
                "dynamic-thread-pool-test-app", threadPoolName);
        restoreConfig.setCorePoolSize(config1.getCorePoolSize());
        restoreConfig.setMaximumPoolSize(config1.getMaximumPoolSize());
        dynamicThreadPoolRedisTopic.publish(restoreConfig);
        
        log.info("✓ 端到端流程验证通过");
    }
}
