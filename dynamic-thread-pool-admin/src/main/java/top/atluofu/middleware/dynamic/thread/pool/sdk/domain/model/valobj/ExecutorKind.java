package top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj;

/**
 * @author 有罗敷的马同学
 * @description 执行器类型枚举值对象
 * @Date 上午12:20 2026/6/30
 **/
public enum ExecutorKind {

    PLATFORM_THREAD_POOL,
    SPRING_THREAD_POOL_TASK_EXECUTOR,
    VIRTUAL_THREAD_PER_TASK,
    UNKNOWN

}
