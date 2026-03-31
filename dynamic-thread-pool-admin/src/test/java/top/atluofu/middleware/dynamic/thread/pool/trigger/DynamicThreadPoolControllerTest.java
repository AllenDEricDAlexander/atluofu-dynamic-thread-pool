package top.atluofu.middleware.dynamic.thread.pool.trigger;

import com.alibaba.fastjson2.JSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.types.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * @ClassName: DynamicThreadPoolControllerTest
 * @description: 动态线程池 Controller 单元测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicThreadPoolControllerTest {

    @Mock
    private RedissonClient mockRedissonClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private RMap mockRMap;

    @Mock
    private RBucket mockRBucket;

    @Mock
    private RTopic mockRTopic;

    @InjectMocks
    private DynamicThreadPoolController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 测试 1：查询线程池列表 - 成功
     */
    @Test
    public void test_queryThreadPoolList_Success() {
        List<ThreadPoolConfigEntity> mockList = new ArrayList<>();
        ThreadPoolConfigEntity entity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        entity.setCorePoolSize(10);
        entity.setMaximumPoolSize(50);
        mockList.add(entity);

        doReturn(mockRMap).when(mockRedissonClient).getMap("THREAD_POOL_CONFIG_LIST_KEY");
        Map<String, String> mapData = new HashMap<>();
        mapData.put("test-app", JSON.toJSONString(mockList));
        doReturn(mapData.values()).when(mockRMap).values();

        Response<List<ThreadPoolConfigEntity>> response = controller.queryThreadPoolList();

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertEquals(Response.Code.SUCCESS.getInfo(), response.getInfo());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("test-app", response.getData().get(0).getAppName());
    }

    /**
     * 测试 2：查询线程池列表 - 异常
     */
    @Test
    public void test_queryThreadPoolList_Exception() {
        doThrow(new RuntimeException("Redis 连接失败")).when(mockRedissonClient).getMap("THREAD_POOL_CONFIG_LIST_KEY");

        Response<List<ThreadPoolConfigEntity>> response = controller.queryThreadPoolList();

        assertEquals(Response.Code.UN_ERROR.getCode(), response.getCode());
        assertEquals(Response.Code.UN_ERROR.getInfo(), response.getInfo());
    }

    /**
     * 测试 3：查询线程池配置 - 成功
     */
    @Test
    public void test_queryThreadPoolConfig_Success() {
        ThreadPoolConfigEntity entity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        entity.setCorePoolSize(20);
        entity.setMaximumPoolSize(80);
        entity.setActiveCount(5);

        String cacheKey = "THREAD_POOL_CONFIG_PARAMETER_LIST_KEY_test-app_threadPoolExecutor01";
        when(mockRedissonClient.getBucket(anyString())).thenReturn(mockRBucket);
        when(mockRBucket.get()).thenReturn(entity);

        Response<ThreadPoolConfigEntity> response = controller.queryThreadPoolConfig("test-app", "threadPoolExecutor01");

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertNotNull(response.getData());
        assertEquals(20, response.getData().getCorePoolSize());
        assertEquals(80, response.getData().getMaximumPoolSize());
        assertEquals(5, response.getData().getActiveCount());
    }

    /**
     * 测试 4：查询线程池配置 - 配置不存在
     */
    @Test
    public void test_queryThreadPoolConfig_NotFound() {
        String cacheKey = "THREAD_POOL_CONFIG_PARAMETER_LIST_KEY_test-app_nonExistent";
        when(mockRedissonClient.getBucket(anyString())).thenReturn(mockRBucket);
        when(mockRBucket.get()).thenReturn(null);

        Response<ThreadPoolConfigEntity> response = controller.queryThreadPoolConfig("test-app", "nonExistent");

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertNull(response.getData());
    }

    /**
     * 测试 5：查询线程池配置 - 异常
     */
    @Test
    public void test_queryThreadPoolConfig_Exception() {
        String cacheKey = "THREAD_POOL_CONFIG_PARAMETER_LIST_KEY_test-app_error";
        when(mockRedissonClient.getBucket(anyString())).thenThrow(new RuntimeException("Redis 错误"));

        Response<ThreadPoolConfigEntity> response = controller.queryThreadPoolConfig("test-app", "error");

        assertEquals(Response.Code.UN_ERROR.getCode(), response.getCode());
    }

    /**
     * 测试 6：更新线程池配置 - 成功
     */
    @Test
    public void test_updateThreadPoolConfig_Success() {
        ThreadPoolConfigEntity request = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        request.setCorePoolSize(30);
        request.setMaximumPoolSize(100);

        String topicKey = "DYNAMIC_THREAD_POOL_REDIS_TOPIC_test-app";
        when(mockRedissonClient.getTopic(anyString())).thenReturn(mockRTopic);
        when(mockRTopic.publish(anyString())).thenReturn(1L);

        Response<Boolean> response = controller.updateThreadPoolConfig(request);

        assertEquals(Response.Code.SUCCESS.getCode(), response.getCode());
        assertTrue(response.getData());
        verify(mockRTopic, times(1)).publish(anyString());
    }

    /**
     * 测试 7：更新线程池配置 - 异常
     */
    @Test
    public void test_updateThreadPoolConfig_Exception() {
        ThreadPoolConfigEntity request = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        request.setCorePoolSize(30);

        String topicKey = "DYNAMIC_THREAD_POOL_REDIS_TOPIC_test-app";
        when(mockRedissonClient.getTopic(anyString())).thenThrow(new RuntimeException("Redis 发布失败"));

        Response<Boolean> response = controller.updateThreadPoolConfig(request);

        assertEquals(Response.Code.UN_ERROR.getCode(), response.getCode());
        assertFalse(response.getData());
    }

    /**
     * 测试 8：验证请求 JSON 序列化
     */
    @Test
    public void test_RequestJsonSerialization() {
        ThreadPoolConfigEntity entity = new ThreadPoolConfigEntity("test-app", "threadPoolExecutor01");
        entity.setCorePoolSize(20);
        entity.setMaximumPoolSize(50);

        String json = JSON.toJSONString(entity);

        assertNotNull(json);
        assertTrue(json.contains("appName"));
        assertTrue(json.contains("threadPoolName"));
        assertTrue(json.contains("corePoolSize"));
        assertTrue(json.contains("maximumPoolSize"));
    }
}
