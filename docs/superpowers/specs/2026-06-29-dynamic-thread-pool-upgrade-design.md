# Atluofu Dynamic Thread Pool Upgrade Design

## Context

The current project is a Spring Boot 3.1 / Java 17 dynamic thread pool tool with five active modules:

- `atluofu-dynamic-thread-pool-spring-boot-starter`
- `dynamic-thread-pool-admin`
- `atluofu-dynamic-thread-pool-ui`
- `atluofu-dynamic-thread-pool-test`
- `atluofu-dynamic-thread-pool-test2`

The upgrade requirements move the project toward a JDK 21 and Spring Boot 3.5 executor governance framework. The implementation must stay in the existing modules and package families. No new `core`, `registry-redis`, or `boot3-starter` module will be created in this round.

## Goals

This upgrade must deliver:

- JDK 21 compilation and runtime support.
- Spring Boot 3.5.x dependency and auto-configuration support.
- Removal of the old `dynamic.thread.pool.config` prefix.
- A single new configuration prefix: `atluofu.dynamic.thread-pool`.
- New REST-style Admin API only; old underscore APIs are removed.
- A `ManagedExecutor` abstraction for platform pools, Spring task executors, and bounded virtual-thread executors.
- Safe traditional thread pool resize behavior.
- User-declared bounded virtual-thread executor support.
- MDC, `traceId`, and `requestId` propagation for managed executor paths.
- Redis message trace propagation.
- Redis key isolation by `appName`, `instanceId`, and `executorName`.
- Audit events written to Redis and queryable through Admin.
- Micrometer metrics as a required acceptance item.
- Minimal usable UI adaptation for the new API and executor model.

## Non-Goals

This round will not include:

- Module split or package-wide architecture relocation.
- Automatic creation of a default virtual-thread executor bean.
- Rollback execution.
- RBAC or full authentication.
- Multi-registry support.
- Automatic thread pool tuning.
- Prometheus or Grafana dashboard artifacts.
- Broad UI redesign beyond required field and API adaptation.

## Configuration

Only this prefix is supported:

```yaml
atluofu:
  dynamic:
    thread-pool:
      enabled: true
      app-name: ${spring.application.name}
      instance-id: ${spring.application.name}-${server.port}
      registry:
        type: redis
        redis:
          host: 127.0.0.1
          port: 6379
          password:
          database: 0
      report:
        enabled: true
        interval: 20s
      trace:
        enabled: true
        mdc-enabled: true
        trace-id-key: traceId
        request-id-key: requestId
      virtual:
        enabled: true
        default-concurrency-limit: 500
```

The old `dynamic.thread.pool.config` prefix is deleted rather than kept as a compatibility alias. Test applications and README examples must be migrated to the new prefix.

`app-name` defaults to `spring.application.name`. `instance-id` defaults to a stable value derived from application name and server port. If a port is not available, the starter may use a local process or host fallback.

## Module Strategy

All changes stay in existing modules:

- Starter: configuration, executor abstraction, context propagation, Redis registry, metrics, listener, reporting.
- Admin: REST API, trace filter, audit event query, Redis read/write.
- UI: API migration and minimal executor-kind rendering.
- Test apps: JDK 21 / Boot 3.5 compatibility and sample platform / virtual executor declarations.

New code should remain under the existing `top.atluofu.middleware.dynamic.thread.pool` package family and follow the current comment and naming style unless Spring Boot 3.5 conventions require a specific annotation or file name.

## Core Model

The starter introduces a local executor governance model:

```java
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
```

`ExecutorKind` values:

```java
PLATFORM_THREAD_POOL
SPRING_THREAD_POOL_TASK_EXECUTOR
VIRTUAL_THREAD_PER_TASK
UNKNOWN
```

`ExecutorSnapshot` is the shared status model used by Redis, Admin, UI, metrics, and tests. Traditional thread pool fields are nullable for virtual-thread executors. Virtual-thread fields are nullable for traditional executors.

`ExecutorUpdateCommand` is the shared update command model for resize and virtual concurrency limit changes. It includes `traceId`, `requestId`, `operator`, `appName`, `instanceId`, `executorName`, and `executorKind`.

## Design Patterns

Adapter is used for executor type differences:

- `ThreadPoolExecutorManagedExecutor`
- `ThreadPoolTaskExecutorManagedExecutor`
- `BoundedVirtualThreadManagedExecutor`

This isolates type-specific snapshot and update behavior while keeping Admin, Redis, UI, and metrics on the shared `ManagedExecutor` contract.

Decorator is used for context propagation:

- `DtpRunnable`
- `DtpCallable`
- `DtpSupplier`
- `DtpContextAwareExecutorService`
- `DtpTaskDecorator`

This keeps MDC capture and restore behavior centralized and prevents each executor adapter from reimplementing the same wrapping rules.

Registry is used for local executor lookup:

- `ManagedExecutorRegistry` indexes executors by `executorName`.
- The Redis registry stores snapshots, events, and publishes update messages.

No additional Factory or Strategy layer is introduced in this round because the above boundaries cover the current variation points without adding unnecessary call depth.

## Executor Collection

The starter no longer depends directly on `Map<String, ThreadPoolExecutor>` as the service model.

It collects:

- `ThreadPoolExecutor` beans.
- Spring `ThreadPoolTaskExecutor` beans.
- User-declared `BoundedVirtualThreadExecutor` beans.

Each collected executor is adapted into a `ManagedExecutor` and registered in `ManagedExecutorRegistry`.

For virtual threads, the starter only provides `BoundedVirtualThreadExecutor`. Business applications must declare their own virtual-thread executor bean when they want to use virtual threads. The starter must not automatically create a default virtual-thread executor.

## Traditional Thread Pool Resize

Traditional pools support:

- `corePoolSize`
- `maximumPoolSize`
- `keepAliveSeconds`
- `allowCoreThreadTimeOut`
- rejection policy display
- queue type display
- queue size and remaining capacity display

Resize validation rules:

- `corePoolSize > 0`
- `maximumPoolSize > 0`
- `corePoolSize <= maximumPoolSize`
- unsupported executor kinds return an explicit failure result

Core and maximum size updates must use a safe order to avoid `IllegalArgumentException` during transitions.

Queue capacity is displayed but not dynamically resized in this round.

## Virtual Thread Executor

`BoundedVirtualThreadExecutor` provides virtual-thread-per-task execution guarded by a dynamic concurrency limit.

It tracks:

- `concurrencyLimit`
- `runningTasks`
- `submittedTasks`
- `completedTasks`
- `failedTasks`
- `rejectedTasks`
- `availablePermits`
- `threadNamePrefix`

It does not expose traditional pool fields such as `corePoolSize`, `maximumPoolSize`, `poolSize`, or `queueSize`.

Concurrency-limit updates are supported through `ManagedExecutor.update()`.

When the limit is exceeded, the executor rejects the task with `RejectedExecutionException` and increments the rejected counter.

## Trace and MDC

Admin and test applications add a trace filter:

- If `X-Trace-Id` exists, use it.
- Otherwise generate a new trace id.
- If `X-Request-Id` exists, use it.
- Otherwise use the trace id as request id.
- Put both values into MDC.
- Return `X-Trace-Id` in the response header.
- Restore or clear MDC when the request exits.

The starter provides:

- `DtpContextSnapshot`
- `DtpRunnable`
- `DtpCallable`
- `DtpSupplier`
- `DtpContextAwareExecutorService`
- `DtpTaskDecorator`
- `DtpThreads`

Managed executor paths automatically propagate MDC for `execute`, `submit`, and supported `CompletableFuture` paths when a DTP-managed executor is passed explicitly.

Default `CompletableFuture` common-pool usage is not automatically governed. Users must pass a DTP executor or manually wrap with `DtpSupplier` and related helpers.

## Redis Message Contract

Configuration changes use a message envelope instead of publishing raw thread pool configuration entities:

```text
messageId
traceId
requestId
appName
instanceId
executorName
executorKind
payload
operator
timestamp
```

Admin publishes the envelope with the current request trace. SDK listeners restore MDC from the envelope before applying updates and clear MDC after processing.

Failed update handling must:

- keep the existing executor configuration unchanged where possible,
- log the failure reason with trace id,
- write an audit event with `success=false`,
- report the latest snapshot after processing.

## Redis Key Design

The Redis key model is:

```text
DTP:APPS
DTP:APP:{appName}:INSTANCES
DTP:SNAPSHOT:{appName}:{instanceId}:{executorName}
DTP:CHANGE_TOPIC:{appName}
DTP:EVENT:{appName}:{yyyyMMdd}
```

Snapshots are isolated by application, instance, and executor. The global list key used by the current implementation is removed.

Snapshot keys should have an expiry so stale instances can disappear from Admin views after a reasonable window. Event keys can use a longer retention window.

## Audit Events

Audit is required in this round and is backed by Redis.

Each update produces an event with:

```text
eventId
traceId
requestId
appName
instanceId
executorName
executorKind
operator
operationType
beforeValue
afterValue
success
errorMessage
createdAt
```

Admin exposes query support through `GET /api/v1/dtp/events`.

Rollback is not implemented in this round. The event model should retain enough before/after data to support rollback later.

## Micrometer Metrics

Micrometer support is required in this round.

Metrics are registered when Micrometer is on the classpath. The starter should avoid making Actuator a hard dependency unless the project already needs it for sample apps.

Required metric groups:

```text
dtp.executor.active
dtp.executor.pool.size
dtp.executor.pool.core
dtp.executor.pool.max
dtp.executor.queue.size
dtp.executor.queue.remaining
dtp.executor.completed
dtp.executor.rejected
dtp.executor.virtual.running
dtp.executor.virtual.submitted
dtp.executor.virtual.completed
dtp.executor.virtual.failed
dtp.executor.virtual.permits.available
```

Tags:

```text
appName
instanceId
executorName
executorKind
virtual
```

Metric registration must tolerate unavailable executor fields. Traditional-only metrics are not emitted for virtual executors unless they have a meaningful value.

## Admin API

The old `/api/v1/dynamic/thread/pool/*` underscore APIs are removed.

The new API is:

```text
GET  /api/v1/dtp/apps
GET  /api/v1/dtp/apps/{appName}/instances
GET  /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors
GET  /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}
POST /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/resize
POST /api/v1/dtp/apps/{appName}/instances/{instanceId}/executors/{executorName}/virtual-limit
GET  /api/v1/dtp/events
```

All responses include trace id. Validation failures return clear messages and do not publish Redis change messages.

The Admin controller may remain simple in this round, but Redis key construction and message publishing should move into small focused helpers to keep the controller readable.

## UI Minimal Adaptation

The UI keeps its current Vue and Element Plus structure.

Required changes:

- Switch API client to the new REST endpoints.
- Show `appName`, `instanceId`, `executorName`, and `executorKind`.
- For traditional executors, show core, max, active, pool, queue type, queue size, and remaining capacity.
- For virtual executors, show concurrency limit, running, submitted, completed, failed, rejected, and available permits.
- The edit dialog changes by executor kind:
  - Traditional executors call `resize`.
  - Virtual executors call `virtual-limit`.
- If an executor does not support an operation, the UI disables the action.

No visual redesign is required.

## Testing and Validation

Unit tests must cover:

- `ThreadPoolResizeSupport`
- `DtpContextSnapshot`
- `DtpRunnable`
- `DtpCallable`
- `DtpSupplier`
- `DtpContextAwareExecutorService`
- `BoundedVirtualThreadExecutor`
- `ManagedExecutor` adapters
- Redis key generation and audit event writing
- Admin REST validation and response trace id
- Micrometer meter registration

Integration or sample-app tests should cover:

- Starter auto-configuration through `AutoConfiguration.imports`
- New configuration prefix binding
- Traditional executor registration and resize
- User-declared virtual executor registration and limit update
- Redis message trace propagation
- Multi-instance snapshot isolation
- MDC not lost and not leaked across tasks

Expected validation commands:

```bash
mvn clean test
mvn clean package
npm --prefix atluofu-dynamic-thread-pool-ui run build
```

The project should not be started automatically as part of implementation completion.

## Delivery Order

Each implementation task should be committed separately.

1. Upgrade platform and auto-configuration.
2. Replace configuration prefix and update samples.
3. Add managed executor model and traditional executor adapter.
4. Add safe resize and Redis snapshot key isolation.
5. Add trace filter and MDC propagation utilities.
6. Add Redis change envelope and listener trace restore.
7. Add bounded virtual-thread executor and adapter.
8. Add audit event persistence and query API.
9. Add Micrometer metric binding.
10. Replace Admin REST API and adapt UI minimally.
11. Refresh README and verification notes.

## Resolved Decisions

All previously open decisions are resolved:

- No module split.
- Old configuration prefix is deleted.
- Old Admin APIs are removed.
- Audit events are required and queryable from Redis.
- Micrometer metrics are required.
- Virtual-thread executors are business-declared beans.
- UI receives minimal usable adaptation.
