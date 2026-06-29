package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @description DynamicThreadPoolAutoProperties 属性绑定测试
 */
class DynamicThreadPoolAutoPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "spring.application.name=test-app",
                    "server.port=8093",
                    "atluofu.dynamic.thread-pool.enabled=true",
                    "atluofu.dynamic.thread-pool.registry.redis.host=127.0.0.1",
                    "atluofu.dynamic.thread-pool.registry.redis.port=6379",
                    "atluofu.dynamic.thread-pool.registry.redis.password=pwd",
                    "atluofu.dynamic.thread-pool.report.interval=20s",
                    "atluofu.dynamic.thread-pool.trace.trace-id-key=traceId",
                    "atluofu.dynamic.thread-pool.trace.request-id-key=requestId",
                    "atluofu.dynamic.thread-pool.virtual.default-concurrency-limit=500"
            );

    @Test
    void shouldBindNewPrefix() {
        contextRunner.run(context -> {
            DynamicThreadPoolAutoProperties properties = context.getBean(DynamicThreadPoolAutoProperties.class);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getRegistry().getRedis().getHost()).isEqualTo("127.0.0.1");
            assertThat(properties.getRegistry().getRedis().getPort()).isEqualTo(6379);
            assertThat(properties.getRegistry().getRedis().getPassword()).isEqualTo("pwd");
            assertThat(properties.getReport().getInterval()).hasSeconds(20);
            assertThat(properties.getTrace().getTraceIdKey()).isEqualTo("traceId");
            assertThat(properties.getTrace().getRequestIdKey()).isEqualTo("requestId");
            assertThat(properties.getVirtual().getDefaultConcurrencyLimit()).isEqualTo(500);
        });
    }

    @Configuration
    @EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
    static class PropertiesConfiguration {
    }
}
