package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.redis;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @description RedisRegistry 单元测试
 */
public class RedisRegistryTest {

    /**
     * 测试：上报线程池列表
     */
    @Test
    public void test_reportThreadPool() {
        RedissonClient mockRedissonClient = mock(RedissonClient.class);
        @SuppressWarnings("rawtypes")
        RList mockRList = mock(RList.class);
        RedisRegistry redisRegistry = new RedisRegistry(mockRedissonClient);

        ThreadPoolConfigEntity e1 = new ThreadPoolConfigEntity("app", "pool1");
        ThreadPoolConfigEntity e2 = new ThreadPoolConfigEntity("app", "pool2");
        List<ThreadPoolConfigEntity> list = Arrays.asList(e1, e2);

        when(mockRedissonClient.getList(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey()))
                .thenReturn(mockRList);

        redisRegistry.reportThreadPool(list);

        verify(mockRedissonClient, times(1))
                .getList(RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey());
        verify(mockRList, times(1)).delete();
        verify(mockRList, times(1)).addAll(list);
    }

    /**
     * 测试：上报单个线程池配置
     */
    @Test
    public void test_reportThreadPoolConfigParameter() {
        RedissonClient mockRedissonClient = mock(RedissonClient.class);
        @SuppressWarnings("rawtypes")
        RBucket mockRBucket = mock(RBucket.class);
        RedisRegistry redisRegistry = new RedisRegistry(mockRedissonClient);

        ThreadPoolConfigEntity entity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        entity.setCorePoolSize(10);
        entity.setMaximumPoolSize(50);

        String expectedKey = RegistryEnumVO.THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey()
                + "_" + entity.getAppName()
                + "_" + entity.getThreadPoolName();

        when(mockRedissonClient.getBucket(expectedKey)).thenReturn(mockRBucket);

        redisRegistry.reportThreadPoolConfigParameter(entity);

        verify(mockRedissonClient, times(1)).getBucket(expectedKey);
        verify(mockRBucket, times(1)).set(eq(entity), any(Duration.class));
    }
}

