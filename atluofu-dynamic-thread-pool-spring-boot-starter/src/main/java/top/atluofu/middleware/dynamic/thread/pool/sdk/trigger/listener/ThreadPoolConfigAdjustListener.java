package top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.listener;

import com.alibaba.fastjson2.JSON;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import java.util.List;

/**
 * @BelongsProject: atluofu-dynamic-thread-pool
 * @BelongsPackage: top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.listener
 * @ClassName: ThreadPoolConfigAdjustListener
 * @Author: atluofu
 * @CreateTime: 2025Year-05Month-11Day-下午 3:39
 * @Description: 动态线程池变更监听
 * @Version: 1.0
 */
public class ThreadPoolConfigAdjustListener implements MessageListener<ThreadPoolConfigEntity> {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolConfigAdjustListener.class);

    private final String applicationName;

    private final IDynamicThreadPoolService dynamicThreadPoolService;

    private final IRegistry registry;

    public ThreadPoolConfigAdjustListener(String applicationName, IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.applicationName = applicationName;
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }

    @Override
    public void onMessage(CharSequence charSequence, ThreadPoolConfigEntity threadPoolConfigEntity) {
        try {
            logger.info("动态线程池，收到配置变更消息。线程池名称:{} 核心线程数:{} 最大线程数:{}", 
                threadPoolConfigEntity.getThreadPoolName(), 
                threadPoolConfigEntity.getCorePoolSize(), 
                threadPoolConfigEntity.getMaximumPoolSize());
            
            // 1. 先更新线程池配置
            dynamicThreadPoolService.updateThreadPoolConfig(threadPoolConfigEntity);
            logger.info("动态线程池，配置更新完成。线程池名称:{}", threadPoolConfigEntity.getThreadPoolName());

            // 2. 无论更新是否成功，都按应用名上报最新数据（原子 Hash 操作，不影响其他应用）
            List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
            registry.reportThreadPoolByApp(applicationName, threadPoolConfigEntities);
            logger.info("动态线程池，上报线程池列表完成。应用:{} 数量:{}", applicationName, threadPoolConfigEntities.size());

            // 3. 上报单个线程池配置
            ThreadPoolConfigEntity currentConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(
                threadPoolConfigEntity.getThreadPoolName());
            registry.reportThreadPoolConfigParameter(currentConfig);
            logger.info("动态线程池，上报线程池配置完成。{}", JSON.toJSONString(currentConfig));
            
        } catch (Exception e) {
            logger.error("动态线程池，配置变更处理失败。线程池名称:{}", 
                threadPoolConfigEntity.getThreadPoolName(), e);
            
            // 4. 即使出错也要按应用名上报当前状态，保证监控不中断
            try {
                List<ThreadPoolConfigEntity> entities = dynamicThreadPoolService.queryThreadPoolList();
                registry.reportThreadPoolByApp(applicationName, entities);
                logger.warn("动态线程池，异常后上报当前状态。应用:{} 数量:{}", applicationName, entities.size());
            } catch (Exception ex) {
                logger.error("动态线程池，异常后上报状态也失败", ex);
            }
        }
    }

}
