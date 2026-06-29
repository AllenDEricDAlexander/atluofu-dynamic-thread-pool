package top.atluofu.middleware.dynamic.thread.pool.sdk.registry;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model.DtpAuditEvent;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model.DtpConfigChangeMessage;

import java.util.List;

/**
 * @ClassName: IRegistry
 * @description: 注册中心接口
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:14
 * @Version: 1.0
 */
public interface IRegistry {

    void reportSnapshot(ExecutorSnapshot snapshot);

    void reportSnapshots(List<ExecutorSnapshot> snapshots);

    List<ExecutorSnapshot> querySnapshots(String appName, String instanceId);

    ExecutorSnapshot querySnapshot(String appName, String instanceId, String executorName);

    void publishConfigChange(DtpConfigChangeMessage message);

    void recordAuditEvent(DtpAuditEvent event);

    List<DtpAuditEvent> queryAuditEvents(String appName, String date);

    @Deprecated
    default void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities) {
    }

    @Deprecated
    default void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
    }

}
