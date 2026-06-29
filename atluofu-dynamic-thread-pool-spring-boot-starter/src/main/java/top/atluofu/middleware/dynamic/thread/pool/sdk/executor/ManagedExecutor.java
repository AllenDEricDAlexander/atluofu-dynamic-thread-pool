package top.atluofu.middleware.dynamic.thread.pool.sdk.executor;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

/**
 * @author      有罗敷的马同学
 * @description 托管执行器接口
 * @Date        上午8:55 2026/6/29
 **/
public interface ManagedExecutor {

    String appName();

    String instanceId();

    String executorName();

    ExecutorKind kind();

    ExecutorSnapshot snapshot();

    UpdateResult update(ExecutorUpdateCommand command);

    boolean supportsResize();

    boolean supportsVirtualThread();

    boolean supportsQueueMetrics();

}
