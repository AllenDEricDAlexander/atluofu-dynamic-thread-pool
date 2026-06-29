package top.atluofu.middleware.dynamic.thread.pool.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.atluofu.config.ThreadPoolConfig;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutorRegistry;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter.BoundedVirtualThreadManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.adapter.ThreadPoolExecutorManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @description 第二个示例应用执行器注册轻量测试
 */
public class Sample2ExecutorRegistrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ThreadPoolConfig.class);

    @Test
    public void test_virtualExecutorShouldRegisterWithoutRedis() {
        contextRunner.run(context -> {
            List<ManagedExecutor> managedExecutors = new ArrayList<>();
            context.getBeansOfType(ThreadPoolExecutor.class).forEach((executorName, executor) ->
                    managedExecutors.add(new ThreadPoolExecutorManagedExecutor("sample-app2", "sample-instance2", executorName, executor)));
            context.getBeansOfType(BoundedVirtualThreadExecutor.class).forEach((executorName, executor) ->
                    managedExecutors.add(new BoundedVirtualThreadManagedExecutor("sample-app2", "sample-instance2", executorName, executor)));
            DynamicThreadPoolService service = new DynamicThreadPoolService(new ManagedExecutorRegistry(managedExecutors));

            List<ExecutorSnapshot> snapshots = service.queryExecutorSnapshots();

            assertThat(snapshots).anySatisfy(snapshot -> {
                assertThat(snapshot.getExecutorName()).isEqualTo("virtualTaskExecutor");
                assertThat(snapshot.getExecutorKind()).isEqualTo(ExecutorKind.VIRTUAL_THREAD_PER_TASK);
                assertThat(snapshot.isVirtual()).isTrue();
                assertThat(snapshot.getConcurrencyLimit()).isEqualTo(100);
            });
            assertThat(snapshots).anySatisfy(snapshot -> {
                assertThat(snapshot.getExecutorName()).isEqualTo("threadPoolExecutor01");
                assertThat(snapshot.getExecutorKind()).isEqualTo(ExecutorKind.PLATFORM_THREAD_POOL);
                assertThat(snapshot.getCorePoolSize()).isPositive();
                assertThat(snapshot.getMaximumPoolSize()).isGreaterThanOrEqualTo(snapshot.getCorePoolSize());
            });
        });
    }

}
