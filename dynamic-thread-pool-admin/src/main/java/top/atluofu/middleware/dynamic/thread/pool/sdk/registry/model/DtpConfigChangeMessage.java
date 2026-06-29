package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorUpdateCommand;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.time.Instant;

/**
 * @author      有罗敷的马同学
 * @description 动态线程池配置变更消息
 * @Date        上午12:20 2026/6/30
 **/
@Getter
@Setter
@ToString
public class DtpConfigChangeMessage {

    private String messageId;

    private String traceId;

    private String requestId;

    private String appName;

    private String instanceId;

    private String executorName;

    private ExecutorKind executorKind;

    private ExecutorUpdateCommand payload;

    private String operator;

    private Instant timestamp;

}
