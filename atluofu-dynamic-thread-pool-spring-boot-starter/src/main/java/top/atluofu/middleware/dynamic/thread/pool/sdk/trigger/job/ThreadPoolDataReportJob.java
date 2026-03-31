package top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.job;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import java.util.List;

/**
 * @ClassName: ThreadPoolDataReportJob
 * @description: 线程池数据上报任务
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午 8:17
 * @Version: 1.0
 */
public class ThreadPoolDataReportJob {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolDataReportJob.class);

    private final IDynamicThreadPoolService dynamicThreadPoolService;

    private final IRegistry registry;

    public ThreadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void execReportThreadPoolList() {
        try {
            // 1. 查询所有线程池状态
            List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
            
            // 2. 上报线程池列表
            registry.reportThreadPool(threadPoolConfigEntities);
            logger.info("动态线程池，定时上报线程池列表。数量:{}", threadPoolConfigEntities.size());

            // 3. 逐个上报线程池配置（单个失败不影响其他）
            for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
                try {
                    registry.reportThreadPoolConfigParameter(threadPoolConfigEntity);
                    logger.debug("动态线程池，定时上报线程池配置。{}", JSON.toJSONString(threadPoolConfigEntity));
                } catch (Exception e) {
                    logger.error("动态线程池，定时上报单个线程池配置失败。线程池：{}", 
                        threadPoolConfigEntity.getThreadPoolName(), e);
                    // 继续上报下一个，不中断整个流程
                }
            }
            
            logger.info("动态线程池，定时上报完成。成功上报 {}/{} 个线程池", 
                threadPoolConfigEntities.size(), threadPoolConfigEntities.size());
                
        } catch (Exception e) {
            logger.error("动态线程池，定时上报线程池列表失败", e);
            // 不抛出异常，避免影响下一次定时执行
        }
    }

}
