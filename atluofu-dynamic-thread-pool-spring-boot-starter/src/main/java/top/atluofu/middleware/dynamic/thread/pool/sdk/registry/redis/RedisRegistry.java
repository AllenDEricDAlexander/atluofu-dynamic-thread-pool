package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.redis;

import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import com.alibaba.fastjson2.JSON;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: RedisRegistry
 * @description: Redis 注册中心
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:15
 * @Version: 1.0
 */
public class RedisRegistry implements IRegistry {

    private final RedissonClient redissonClient;

    public RedisRegistry(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities) {
        RList<ThreadPoolConfigEntity> list = redissonClient.getList(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        list.delete();
        list.addAll(threadPoolEntities);
    }

    @Override
    public void reportThreadPoolByApp(String appName, List<ThreadPoolConfigEntity> threadPoolEntities) {
        // 使用 RMap 按应用名隔离数据，key 为 appName，value 为 JSON 序列化的列表
        // 每次只更新单个应用的数据，其他应用数据不受影响，原子性由 Redis 保证
        RMap<String, String> map = redissonClient.getMap(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        map.put(appName, JSON.toJSONString(threadPoolEntities));
        map.expire(1, TimeUnit.DAYS);
    }

    /**
     * 按应用名从 Redis Hash 中读取线程池列表
     */
    public List<ThreadPoolConfigEntity> getThreadPoolByApp(String appName) {
        RMap<String, String> map = redissonClient.getMap(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        String json = map.get(appName);
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(json, ThreadPoolConfigEntity.class);
    }

    /**
     * 从 Redis Hash 中读取所有应用的线程池列表（兼容旧接口）
     */
    public List<ThreadPoolConfigEntity> readAllThreadPool() {
        RMap<String, String> map = redissonClient.getMap(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        List<ThreadPoolConfigEntity> result = new ArrayList<>();
        for (String json : map.values()) {
            if (json != null && !json.isEmpty()) {
                result.addAll(JSON.parseArray(json, ThreadPoolConfigEntity.class));
            }
        }
        return result;
    }

    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        String cacheKey = RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + threadPoolConfigEntity.getAppName() + "_" + threadPoolConfigEntity.getThreadPoolName();
        RBucket<ThreadPoolConfigEntity> bucket = redissonClient.getBucket(cacheKey);
        bucket.set(threadPoolConfigEntity, Duration.ofDays(30));
    }

}
