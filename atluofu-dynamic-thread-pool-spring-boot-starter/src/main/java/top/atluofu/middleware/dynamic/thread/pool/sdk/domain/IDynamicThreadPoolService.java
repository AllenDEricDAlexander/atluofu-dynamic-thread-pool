package top.atluofu.middleware.dynamic.thread.pool.sdk.domain;

import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.UpdateResult;

import java.util.List;

/**
 * @author 有罗敷的马同学
 * @description 动态线程池服务
 * @Date 上午8:56 2025/4/13
 **/
public interface IDynamicThreadPoolService {

    List<ExecutorSnapshot> queryExecutorSnapshots();

    ExecutorSnapshot queryExecutorSnapshot(String executorName);

    UpdateResult updateExecutor(ExecutorUpdateCommand command);

}
