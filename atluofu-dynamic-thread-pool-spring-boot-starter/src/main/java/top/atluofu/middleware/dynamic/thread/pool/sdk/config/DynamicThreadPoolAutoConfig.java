package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.redis.RedisRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.job.ThreadPoolDataReportJob;
import top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.listener.ThreadPoolConfigAdjustListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ClassName: DynamicThreadPoolAutoConfig
 * @description: 动态配置入口
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-01Month-05Day-21:14
 * @Version: 1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@ConditionalOnProperty(prefix = "atluofu.dynamic.thread-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DynamicThreadPoolAutoConfig {

    private String applicationName;

    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();

        // 使用自定义 ObjectMapper 的 JsonJacksonCodec
        config.setCodec(new JsonJacksonCodec(createRedisObjectMapper()));

        DynamicThreadPoolAutoProperties.Redis redis = properties.getRegistry().getRedis();
        config.useSingleServer()
                .setAddress("redis://" + redis.getHost() + ":" + redis.getPort())
                .setPassword(redis.getPassword())
                .setDatabase(redis.getDatabase())
                .setConnectionPoolSize(redis.getPoolSize())
                .setConnectionMinimumIdleSize(redis.getMinIdleSize())
                .setIdleConnectionTimeout(redis.getIdleTimeout())
                .setConnectTimeout(redis.getConnectTimeout())
                .setRetryAttempts(redis.getRetryAttempts())
                .setRetryInterval(redis.getRetryInterval())
                .setPingConnectionInterval(redis.getPingInterval())
                .setKeepAlive(redis.isKeepAlive())
        ;

        RedissonClient redissonClient = Redisson.create(config);
        log.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", redis.getHost(), redis.getPoolSize(), !redissonClient.isShutdown());
        return redissonClient;
    }

    static ObjectMapper createRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }

    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry);
    }

    @Bean(name = "dynamicThreadPoolRedisTopic")
    @DependsOn("dynamicThreadPollService")
    public RTopic dynamicThreadPoolRedisTopic(RedissonClient redissonClient, ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener) {
        RTopic topic = redissonClient.getTopic(RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + applicationName);
        topic.addListener(ThreadPoolConfigEntity.class, threadPoolConfigAdjustListener);
        return topic;
    }

    /**
     * @description:
     * @author: atluofu
     * @date: 2025/4/15 下午2:55
     * @param: [dynamicThreadRedissonClient]
     * @return: top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry
     **/
    @Bean
    public IRegistry redisRegistry(RedissonClient dynamicThreadRedissonClient) {
        return new RedisRegistry(dynamicThreadRedissonClient);
    }

    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        return new ThreadPoolDataReportJob(dynamicThreadPoolService, registry);
    }

    @Bean("dynamicThreadPollService")
    public DynamicThreadPoolService dynamicThreadPollService(ApplicationContext applicationContext, Map<String, ThreadPoolExecutor> threadPoolExecutorMap, RedissonClient redissonClient) {
        applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");

        if (StringUtils.isBlank(applicationName)) {
            applicationName = "缺省的";
            log.warn("动态线程池，启动提示。SpringBoot 应用未配置 spring.application.name 无法获取到应用名称！");
        }

        // 获取缓存数据，设置本地线程池配置
        Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
        for (String threadPoolKey : threadPoolKeys) {
            ThreadPoolConfigEntity threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + applicationName + "_" + threadPoolKey).get();
            if (null == threadPoolConfigEntity) {
                continue;
            }
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
            threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
            threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
        }

        return new DynamicThreadPoolService(applicationName, threadPoolExecutorMap);
    }

}
