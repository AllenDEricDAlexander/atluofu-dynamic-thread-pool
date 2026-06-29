package top.atluofu.middleware.dynamic.thread.pool.sdk.trigger.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.registry.IRegistry;

import java.util.List;

/**
 * @ClassName: ThreadPoolDataReportJob
 * @description: 线程池数据上报任务
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午 8:17
 * @Version: 1.0
 */
public class ThreadPoolDataReportJob {

    private final Logger logger = LoggerFactory.getLogger(ThreadPoolDataReportJob.class);

    private final IDynamicThreadPoolService dynamicThreadPoolService;

    private final IRegistry registry;

    public ThreadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${atluofu.dynamic.thread-pool.report.interval:20s}")
    public void execReportThreadPoolList() {
        try {
            List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
            registry.reportSnapshots(snapshots);
            logger.info("动态线程池，定时上报执行器快照。数量:{}", snapshots.size());
        } catch (Exception e) {
            logger.error("动态线程池，定时上报执行器快照失败", e);
        }
    }

}
