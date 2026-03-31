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

    /**
     * 实现接口方法，但实际业务逻辑由 onRawMessage 处理
     */
    @Override
    public void onMessage(CharSequence channel, ThreadPoolConfigEntity msg) {
        // 此方法不会被直接调用，由 AutoConfig 注册的 addListener(String.class) 回调 onRawMessage
    }

    /**
     * 接收原始 JSON 字符串，由 AutoConfig 通过 addListener(String.class) 注册
     */
    public void onRawMessage(String json) {
        try {
            ThreadPoolConfigEntity threadPoolConfigEntity = JSON.parseObject(json, ThreadPoolConfigEntity.class);

            int corePoolSize = threadPoolConfigEntity.getCorePoolSize();
            int maximumPoolSize = threadPoolConfigEntity.getMaximumPoolSize();

            logger.info("动态线程池，收到配置变更消息。线程池名称:{} 核心线程数:{} 最大线程数:{}",
                threadPoolConfigEntity.getThreadPoolName(), corePoolSize, maximumPoolSize);

            if (threadPoolConfigEntity.getThreadPoolName() == null) {
                logger.error("动态线程池，线程池名称为空。JSON:{}", json);
                return;
            }
            if (corePoolSize <= 0 || maximumPoolSize <= 0) {
                logger.error("动态线程池，配置变更参数无效。corePoolSize={}, maximumPoolSize={}", corePoolSize, maximumPoolSize);
                return;
            }
            if (corePoolSize > maximumPoolSize) {
                logger.error("动态线程池，配置变更参数无效。核心线程数({})不能大于最大线程数({})", corePoolSize, maximumPoolSize);
                return;
            }

            // 1. 更新线程池配置
            dynamicThreadPoolService.updateThreadPoolConfig(threadPoolConfigEntity);
            logger.info("动态线程池，配置更新完成。线程池名称:{}", threadPoolConfigEntity.getThreadPoolName());

            // 2. 按应用名上报最新数据
            List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
            registry.reportThreadPoolByApp(applicationName, threadPoolConfigEntities);
            logger.info("动态线程池，上报线程池列表完成。应用:{} 数量:{}", applicationName, threadPoolConfigEntities.size());

            // 3. 上报单个线程池配置
            ThreadPoolConfigEntity currentConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(
                threadPoolConfigEntity.getThreadPoolName());
            registry.reportThreadPoolConfigParameter(currentConfig);
            logger.info("动态线程池，上报线程池配置完成。{}", JSON.toJSONString(currentConfig));

        } catch (Exception e) {
            logger.error("动态线程池，配置变更处理失败。原始JSON:{}", json, e);
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
