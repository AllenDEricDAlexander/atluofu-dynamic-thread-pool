package top.atluofu.middleware.dynamic.thread.pool.sdk.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutorRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.metrics.DtpMeterBinder;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.job.ThreadPoolDataReportJob;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @description 动态线程池自动配置单元测试
 */
public class DynamicThreadPoolAutoConfigTest {

    private final ApplicationContextRunner metricsContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.DtpMetricsConfiguration.class))
            .withUserConfiguration(ManagedExecutorRegistryConfiguration.class);

    private final ApplicationContextRunner reportContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.DtpReportConfiguration.class))
            .withBean(IDynamicThreadPoolService.class, () -> mock(IDynamicThreadPoolService.class))
            .withBean(IRegistry.class, () -> mock(IRegistry.class));

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

    @Test
    public void test_threadPoolDataReportJobShouldNotCreateWhenReportDisabled() {
        reportContextRunner
                .withPropertyValues("atluofu.dynamic.thread-pool.report.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ThreadPoolDataReportJob.class)
                );
    }

    @Test
    public void test_threadPoolDataReportJobShouldCreateWhenReportEnabled() {
        reportContextRunner
                .withPropertyValues("atluofu.dynamic.thread-pool.report.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(ThreadPoolDataReportJob.class)
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
