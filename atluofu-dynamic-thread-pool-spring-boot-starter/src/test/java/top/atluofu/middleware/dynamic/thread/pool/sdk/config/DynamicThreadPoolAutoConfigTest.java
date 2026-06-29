package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutorRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.metrics.DtpMeterBinder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description 动态线程池自动配置单元测试
 */
public class DynamicThreadPoolAutoConfigTest {

    private final ApplicationContextRunner metricsContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.DtpMetricsConfiguration.class))
            .withUserConfiguration(ManagedExecutorRegistryConfiguration.class);

    @Test
    public void test_createRedisObjectMapper_serializeInstant() throws Exception {
        ObjectMapper objectMapper = DynamicThreadPoolAutoConfig.createRedisObjectMapper();

        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName("test-app");
        snapshot.setInstanceId("i-001");
        snapshot.setExecutorName("executor01");
        snapshot.setReportTime(Instant.parse("2026-06-29T10:15:30Z"));

        String json = objectMapper.writeValueAsString(snapshot);

        assertTrue(json.contains("reportTime"));
    }

    @Test
    public void test_dtpMeterBinderShouldNotCreateWithoutMeterRegistry() {
        metricsContextRunner.run(context ->
                assertThat(context).doesNotHaveBean(DtpMeterBinder.class)
        );
    }

    @Test
    public void test_dtpMeterBinderShouldCreateWithMeterRegistry() {
        metricsContextRunner.withUserConfiguration(MeterRegistryConfiguration.class)
                .run(context ->
                        assertThat(context).hasSingleBean(DtpMeterBinder.class)
                );
    }

    static class ManagedExecutorRegistryConfiguration {

        @Bean
        public ManagedExecutorRegistry managedExecutorRegistry() {
            return new ManagedExecutorRegistry(List.of());
        }

    }

    static class MeterRegistryConfiguration {

        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

    }

}
