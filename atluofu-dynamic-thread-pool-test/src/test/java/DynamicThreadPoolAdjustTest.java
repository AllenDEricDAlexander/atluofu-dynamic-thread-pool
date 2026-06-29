import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import top.atluofu.Application;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: DynamicThreadPoolAdjustTest
 * @description: 动态线程池调整测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@Slf4j
@SpringBootTest(classes = Application.class)
public class DynamicThreadPoolAdjustTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    /**
     * 测试 1：查询线程池列表
     */
    @Test
    public void test_queryThreadPoolList() {
        log.info("========== 测试 1：查询线程池列表 ==========");
        List<ThreadPoolConfigEntity> threadPoolList = dynamicThreadPoolService.queryThreadPoolList();
        
        for (ThreadPoolConfigEntity entity : threadPoolList) {
            log.info("线程池名称：{}, 核心线程数：{}, 最大线程数：{}, 活跃线程数：{}, 队列大小：{}", 
                    entity.getThreadPoolName(), 
                    entity.getCorePoolSize(), 
                    entity.getMaximumPoolSize(),
                    entity.getActiveCount(),
                    entity.getQueueSize());
        }
        
        log.info("线程池总数：{}", threadPoolList.size());
    }

    /**
     * 测试 2：根据名称查询线程池配置
     */
    @Test
    public void test_queryThreadPoolConfigByName() {
        log.info("========== 测试 2：根据名称查询线程池配置 ==========");
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        
        log.info("线程池名称：{}, 应用名：{}, 核心线程数：{}, 最大线程数：{}", 
                entity.getThreadPoolName(), 
                entity.getAppName(),
                entity.getCorePoolSize(), 
                entity.getMaximumPoolSize());
    }

    /**
     * 测试 3：通过 Redis Topic 动态调整线程池配置
     */
    @Test
    public void test_adjustThreadPoolConfig() throws InterruptedException {
        log.info("========== 测试 3：动态调整线程池配置 ==========");
        
        // 调整前的配置
        ThreadPoolConfigEntity beforeConfig = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        log.info("调整前 - 核心线程数：{}, 最大线程数：{}", 
                beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());
        
        // 发布新的配置
        ThreadPoolConfigEntity newConfig = new ThreadPoolConfigEntity("dynamic-thread-pool-test-app", "threadPoolExecutor01");
        newConfig.setCorePoolSize(30);
        newConfig.setMaximumPoolSize(80);
        
        log.info("发布新配置 - 核心线程数：{}, 最大线程数：{}", 
                newConfig.getCorePoolSize(), newConfig.getMaximumPoolSize());
        dynamicThreadPoolRedisTopic.publish(newConfig);
        
        // 等待配置生效
        Thread.sleep(2000);
        
        // 调整后的配置
        ThreadPoolConfigEntity afterConfig = dynamicThreadPoolService.queryThreadPoolConfigByName("threadPoolExecutor01");
        log.info("调整后 - 核心线程数：{}, 最大线程数：{}", 
                afterConfig.getCorePoolSize(), afterConfig.getMaximumPoolSize());
        
        // 验证调整是否成功
        if (afterConfig.getCorePoolSize() == 30 && afterConfig.getMaximumPoolSize() == 80) {
            log.info("✓ 动态调整成功！");
        } else {
            log.error("✗ 动态调整失败！期望核心线程数=30, 最大线程数=80, 实际核心线程数={}, 最大线程数={}", 
                    afterConfig.getCorePoolSize(), afterConfig.getMaximumPoolSize());
        }
        
        new CountDownLatch(1).await(5, TimeUnit.SECONDS);
    }

    /**
     * 测试 4：查询不存在的线程池
     */
    @Test
    public void test_queryNonExistentThreadPool() {
        log.info("========== 测试 4：查询不存在的线程池 ==========");
        ThreadPoolConfigEntity entity = dynamicThreadPoolService.queryThreadPoolConfigByName("nonExistentPool");
        
        log.info("查询结果 - 应用名：{}, 线程池名称：{}", entity.getAppName(), entity.getThreadPoolName());
        log.info("核心线程数：{}, 最大线程数：{} (应为 0)", entity.getCorePoolSize(), entity.getMaximumPoolSize());
    }

}
