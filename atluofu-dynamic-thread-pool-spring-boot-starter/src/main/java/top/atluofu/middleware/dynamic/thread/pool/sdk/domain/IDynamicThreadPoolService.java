package top.atluofu.middleware.dynamic.thread.pool.sdk.domain;


import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;

/**
 * @author 有罗敷的马同学
 * @description 动态线程池服务
 * @Date 上午8:56 2025/4/13
 **/
public interface IDynamicThreadPoolService {

    List<ThreadPoolConfigEntity> queryThreadPoolList();

    ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName);

    void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity);

}
