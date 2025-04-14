package top.atluofu.middleware.dynamic.thread.pool.sdk.registry;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;

/**
 * @ClassName: IRegistry
 * @description: 注册中心接口
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午8:14
 * @Version: 1.0
 */
public interface IRegistry {

    void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolEntities);

    void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity);

}