package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @description DynamicThreadPoolAutoProperties 简单属性测试
 */
public class DynamicThreadPoolAutoPropertiesTest {

    @Test
    public void test_defaultValuesAndSetters() {
        DynamicThreadPoolAutoProperties properties = new DynamicThreadPoolAutoProperties();

        // 默认值校验
        assertEquals(64, properties.getPoolSize());
        assertEquals(10, properties.getMinIdleSize());
        assertEquals(10000, properties.getIdleTimeout());
        assertEquals(10000, properties.getConnectTimeout());
        assertEquals(3, properties.getRetryAttempts());
        assertEquals(1000, properties.getRetryInterval());
        assertEquals(0, properties.getPingInterval());
        assertTrue(properties.isKeepAlive());

        // setter/getter 校验（挑几个关键字段）
        properties.setEnable(true);
        properties.setHost("127.0.0.1");
        properties.setPort(6379);
        properties.setPassword("pwd");

        assertTrue(properties.isEnable());
        assertEquals("127.0.0.1", properties.getHost());
        assertEquals(6379, properties.getPort());
        assertEquals("pwd", properties.getPassword());
    }
}

