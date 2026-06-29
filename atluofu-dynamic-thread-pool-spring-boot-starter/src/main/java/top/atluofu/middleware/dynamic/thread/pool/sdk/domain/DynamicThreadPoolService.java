package top.atluofu.middleware.dynamic.thread.pool.sdk.domain;

import org.apache.commons.lang.StringUtils;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutor;
import top.atluofu.middleware.dynamic.thread.pool.sdk.executor.ManagedExecutorRegistry;

import java.util.List;

/**
 * @author 有罗敷的马同学
 * @description 动态线程池服务
 * @Date 上午 8:56 2025/4/13
 **/
public class DynamicThreadPoolService implements IDynamicThreadPoolService {

    private final ManagedExecutorRegistry managedExecutorRegistry;

    public DynamicThreadPoolService(ManagedExecutorRegistry managedExecutorRegistry) {
        this.managedExecutorRegistry = managedExecutorRegistry;
    }

    @Override
    public List<ExecutorSnapshot> queryExecutorSnapshots() {
        return managedExecutorRegistry.list().stream()
                .map(ManagedExecutor::snapshot)
                .toList();
    }

    @Override
    public ExecutorSnapshot queryExecutorSnapshot(String executorName) {
        return managedExecutorRegistry.get(executorName)
                .map(ManagedExecutor::snapshot)
                .orElse(null);
    }

    @Override
    public UpdateResult updateExecutor(ExecutorUpdateCommand command) {
        if (command == null || StringUtils.isBlank(command.getExecutorName())) {
            return failure("executorName must not be blank");
        }
        return managedExecutorRegistry.get(command.getExecutorName())
                .map(managedExecutor -> managedExecutor.update(command))
                .orElseGet(() -> failure("executor not found: " + command.getExecutorName()));
    }

    private UpdateResult failure(String message) {
        UpdateResult result = new UpdateResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

}
