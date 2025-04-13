package top.atluofu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @ClassName: ThreadPoolConfigProperties
 * @description: TODO
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-13Day-上午8:42
 * @Version: 1.0
 */
@Data
@ConfigurationProperties(prefix = "thread.pool.executor.config", ignoreInvalidFields = true)
public class ThreadPoolConfigProperties {

    /**
     * 核心线程数
     */
    private Integer corePoolSize = 20;
    /**
     * 最大线程数
     */
    private Integer maxPoolSize = 200;
    /**
     * 最大等待时间
     */
    private Long keepAliveTime = 10L;
    /**
     * 最大队列数
     */
    private Integer blockQueueSize = 5000;
    /*
     * AbortPolicy：丢弃任务并抛出RejectedExecutionException异常。
     * DiscardPolicy：直接丢弃任务，但是不会抛出异常
     * DiscardOldestPolicy：将最早进入队列的任务删除，之后再尝试加入队列的任务被拒绝
     * CallerRunsPolicy：如果任务添加线程池失败，那么主线程自己执行该任务
     * */
    private String policy = "AbortPolicy";

}

