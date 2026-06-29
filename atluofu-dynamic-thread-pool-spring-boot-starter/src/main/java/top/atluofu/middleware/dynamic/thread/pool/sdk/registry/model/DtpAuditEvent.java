package top.atluofu.middleware.dynamic.thread.pool.sdk.registry.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.entity.ExecutorSnapshot;
import top.atluofu.middleware.dynamic.thread.pool.sdk.domain.model.valobj.ExecutorKind;

import java.time.Instant;

/**
 * @author      有罗敷的马同学
 * @description 动态线程池审计事件
 * @Date        下午10:40 2026/6/29
 **/
@Getter
@Setter
@ToString
public class DtpAuditEvent {

    private String eventId;

    private String traceId;

    private String requestId;

    private String appName;

    private String instanceId;

    private String executorName;

    private ExecutorKind executorKind;

    private String operator;

    private String operationType;

    private ExecutorSnapshot beforeValue;

    private ExecutorSnapshot afterValue;

    private boolean success;

    private String errorMessage;

    private Instant createdAt;

}
