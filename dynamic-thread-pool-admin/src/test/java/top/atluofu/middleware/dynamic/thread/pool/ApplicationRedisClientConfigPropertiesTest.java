package top.atluofu.middleware.dynamic.thread.pool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import top.atluofu.middleware.dynamic.thread.pool.sdk.config.DynamicThreadPoolAutoConfig;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model.DtpConfigChangeMessage;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @ClassName: ApplicationRedisClientConfigPropertiesTest
 * @description: Redis 客户端配置属性绑定测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-29Day
 * @Version: 1.0
 */
class ApplicationRedisClientConfigPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "atluofu.dynamic.thread-pool.registry.redis.host=127.0.0.1",
                    "atluofu.dynamic.thread-pool.registry.redis.port=6379",
                    "atluofu.dynamic.thread-pool.registry.redis.password=pwd",
                    "atluofu.dynamic.thread-pool.registry.redis.database=1",
                    "atluofu.dynamic.thread-pool.registry.redis.pool-size=10",
                    "atluofu.dynamic.thread-pool.registry.redis.min-idle-size=5",
                    "atluofu.dynamic.thread-pool.registry.redis.idle-timeout=30000",
                    "atluofu.dynamic.thread-pool.registry.redis.connect-timeout=5000",
                    "atluofu.dynamic.thread-pool.registry.redis.retry-attempts=3",
                    "atluofu.dynamic.thread-pool.registry.redis.retry-interval=1000",
                    "atluofu.dynamic.thread-pool.registry.redis.ping-interval=60000",
                    "atluofu.dynamic.thread-pool.registry.redis.keep-alive=true"
            );

    @Test
    void shouldBindRedisPropertiesFromDynamicThreadPoolPrefix() {
        contextRunner.run(context -> {
            AdminApplication.RedisClientConfigProperties properties = context.getBean(AdminApplication.RedisClientConfigProperties.class);

            assertThat(properties.getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getPort()).isEqualTo(6379);
            assertThat(properties.getPassword()).isEqualTo("pwd");
            assertThat(properties.getDatabase()).isEqualTo(1);
            assertThat(properties.getPoolSize()).isEqualTo(10);
            assertThat(properties.getMinIdleSize()).isEqualTo(5);
            assertThat(properties.getIdleTimeout()).isEqualTo(30000);
            assertThat(properties.getConnectTimeout()).isEqualTo(5000);
            assertThat(properties.getRetryAttempts()).isEqualTo(3);
            assertThat(properties.getRetryInterval()).isEqualTo(1000);
            assertThat(properties.getPingInterval()).isEqualTo(60000);
            assertThat(properties.isKeepAlive()).isTrue();
        });
    }

    @Test
    void shouldSerializeDtpMessageInstantWithAdminRedisObjectMapper() throws Exception {
        ObjectMapper objectMapper = AdminApplication.RedisClientConfig.createRedisObjectMapper();
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setAppName("order-app");
        message.setTimestamp(Instant.parse("2026-06-30T00:00:00Z"));

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).contains("timestamp");
    }

    @Test
    void shouldExcludeStarterAutoConfigurationFromAdminApplication() {
        assertThat(AdminApplication.class.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class).exclude())
                .contains(DynamicThreadPoolAutoConfig.class);
    }

    @Test
    void shouldLimitAdminComponentScanToAdminComponents() {
        assertThat(AdminApplication.class.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class).scanBasePackages())
                .containsExactlyInAnyOrder(
                        "top.atluofu.middleware.dynamic.thread.pool.config",
                        "top.atluofu.middleware.dynamic.thread.pool.trigger"
                );
    }

    @Configuration
    @EnableConfigurationProperties(AdminApplication.RedisClientConfigProperties.class)
    static class PropertiesConfiguration {
    }

}
