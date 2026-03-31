package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@Configuration
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
@Slf4j
public class DynamicThreadPoolAutoConfig {

    private String applicationName;

    /**
     * 从配置中提前获取应用名，供其他 Bean 使用
     */
    @Bean
    public String dynamicThreadPoolApplicationName(ApplicationContext applicationContext) {
        String name = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (StringUtils.isBlank(name)) {
            name = "缺省的";
            log.warn("动态线程池，启动提示。SpringBoot 应用未配置 spring.application.name 无法获取到应用名称！");
        }
        return name;
    }

    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();

        // 创建 ObjectMapper 并配置类型信息
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 使用自定义 ObjectMapper 的 JsonJacksonCodec
        config.setCodec(new JsonJacksonCodec(objectMapper));

        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;

        RedissonClient redissonClient = Redisson.create(config);
        log.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", properties.getHost(), properties.getPoolSize(), !redissonClient.isShutdown());
        return redissonClient;
    }

    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(
            String dynamicThreadPoolApplicationName,
            IDynamicThreadPoolService dynamicThreadPoolService,
            IRegistry registry) {
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolApplicationName, dynamicThreadPoolService, registry);
    }

    @Bean(name = "dynamicThreadPoolRedisTopic")
    public RTopic dynamicThreadPoolRedisTopic(
            String dynamicThreadPoolApplicationName,
            RedissonClient redissonClient,
            ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener) {
        RTopic topic = redissonClient.getTopic(RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + dynamicThreadPoolApplicationName);
        // 使用 String.class 接收原始 JSON，显式实现 MessageListener 接口
        // Admin 端直接发布 JSON 字符串，不依赖 JsonJacksonCodec 序列化
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence channel, String json) {
                threadPoolConfigAdjustListener.onRawMessage(json);
            }
        });
        return topic;
    }

    @Bean
    public IRegistry redisRegistry(RedissonClient dynamicThreadRedissonClient) {
        return new RedisRegistry(dynamicThreadRedissonClient);
    }

    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(
            String dynamicThreadPoolApplicationName,
            IDynamicThreadPoolService dynamicThreadPoolService,
            IRegistry registry) {
        return new ThreadPoolDataReportJob(dynamicThreadPoolApplicationName, dynamicThreadPoolService, registry);
    }

    @Bean("dynamicThreadPollService")
    public DynamicThreadPoolService dynamicThreadPollService(
            String dynamicThreadPoolApplicationName,
            Map<String, ThreadPoolExecutor> threadPoolExecutorMap,
            RedissonClient redissonClient) {

        Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
        for (String threadPoolKey : threadPoolKeys) {
            ThreadPoolConfigEntity threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + dynamicThreadPoolApplicationName + "_" + threadPoolKey).get();
            if (null == threadPoolConfigEntity) {
                continue;
            }
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
            int cachedCore = threadPoolConfigEntity.getCorePoolSize();
            int cachedMax = threadPoolConfigEntity.getMaximumPoolSize();
            // JDK 校验：setMaximumPoolSize 要求 x >= currentCore，setCorePoolSize 要求 x <= currentMax
            if (cachedCore > threadPoolExecutor.getMaximumPoolSize()) {
                threadPoolExecutor.setMaximumPoolSize(cachedMax);
                threadPoolExecutor.setCorePoolSize(cachedCore);
            } else {
                threadPoolExecutor.setCorePoolSize(cachedCore);
                threadPoolExecutor.setMaximumPoolSize(cachedMax);
            }
        }

        return new DynamicThreadPoolService(dynamicThreadPoolApplicationName, threadPoolExecutorMap);
    }

}
